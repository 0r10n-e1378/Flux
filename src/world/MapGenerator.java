package world;

import core.Camera;
import entities.BossBoid;
import entities.EnemyBoid;
import entities.FlankerBoid;
import entities.NormalBoid;
import entities.Wall;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import math.Vector;

public class MapGenerator {
    private static final int CHUNK_SIZE = 512;
    private static final int LOAD_RADIUS = 2;

    private final Map<String, Chunk> loadedChunks = new HashMap<>();
    private final Set<String> removedNormalBoidIds = new HashSet<>();
    private final Set<String> removedEnemyBoidIds = new HashSet<>();
    private final Set<String> generatedChunks = new HashSet<>();
    private final List<NormalBoid> normalBoids = new ArrayList<>();
    private final List<EnemyBoid> enemyBoids = new ArrayList<>();
    private final List<BossBoid> bossBoids = new ArrayList<>();
    private double normalBoidSpawnRate = 1.0;
    private int enemySwarmCount = 0;
    private double enemySwarmMultiplier = 1.0;
    private int flankerSwarmCount = 0;
    private double flankerSwarmMultiplier = 1.0;
    private long lastEnemySpawnTime = 0;
    private long enemySpawnInterval = 12000; // Start with 12 seconds between spawns
    private long gameStartTime = System.currentTimeMillis();
    private int bossSpawnCount = 0;

    public void update(double playerX, double playerY) {
        int centerChunkX = (int) Math.floor(playerX / CHUNK_SIZE);
        int centerChunkY = (int) Math.floor(playerY / CHUNK_SIZE);

        for (int y = centerChunkY - LOAD_RADIUS; y <= centerChunkY + LOAD_RADIUS; y++) {
            for (int x = centerChunkX - LOAD_RADIUS; x <= centerChunkX + LOAD_RADIUS; x++) {
                String key = makeKey(x, y);
                long seed = ((long) x << 32) ^ (y & 0xffffffffL);
                Random rand = new Random(seed);
                boolean generateBoids = !generatedChunks.contains(key);
                if (!loadedChunks.containsKey(key)) {
                    loadedChunks.put(key, createChunk(x, y, rand, generateBoids));
                }
                if (generateBoids) {
                    generatedChunks.add(key);
                }
            }
        }

        loadedChunks.entrySet().removeIf(entry -> {
            String[] parts = entry.getKey().split(":");
            int chunkX = Integer.parseInt(parts[0]);
            int chunkY = Integer.parseInt(parts[1]);
            return Math.abs(chunkX - centerChunkX) > LOAD_RADIUS || Math.abs(chunkY - centerChunkY) > LOAD_RADIUS;
        });
    }

    public void draw(Graphics g, Camera camera) {
        for (Chunk chunk : loadedChunks.values()) {
            chunk.draw(g, camera);
        }
    }

    public boolean collidesWithWall(double x, double y, int radius) {
        for (Chunk chunk : loadedChunks.values()) {
            for (Wall wall : chunk.walls) {
                Rectangle bounds = wall.getBounds();
                if (circleIntersectsRect(x, y, radius, bounds)) {
                    return true;
                }
            }
        }
        return false;
    }

    public ArrayList<NormalBoid> getLoadedNormalBoids() {
        ArrayList<NormalBoid> result = new ArrayList<>();
        for (NormalBoid normal : normalBoids) {
            int chunkX = (int) Math.floor(normal.getPosition().x / CHUNK_SIZE);
            int chunkY = (int) Math.floor(normal.getPosition().y / CHUNK_SIZE);
            if (loadedChunks.containsKey(makeKey(chunkX, chunkY))) {
                result.add(normal);
            }
        }
        return result;
    }

    public ArrayList<EnemyBoid> getLoadedEnemyBoids() {
        ArrayList<EnemyBoid> result = new ArrayList<>();
        for (EnemyBoid enemy : enemyBoids) {
            int chunkX = (int) Math.floor(enemy.getPosition().x / CHUNK_SIZE);
            int chunkY = (int) Math.floor(enemy.getPosition().y / CHUNK_SIZE);
            if (loadedChunks.containsKey(makeKey(chunkX, chunkY))) {
                result.add(enemy);
            }
        }
        return result;
    }

    public void removeNormalBoid(NormalBoid boid) {
        removedNormalBoidIds.add(boid.getId());
        normalBoids.remove(boid);
    }

    public ArrayList<BossBoid> getLoadedBossBoids() {
        ArrayList<BossBoid> result = new ArrayList<>();
        for (BossBoid boss : bossBoids) {
            int chunkX = (int) Math.floor(boss.getPosition().x / CHUNK_SIZE);
            int chunkY = (int) Math.floor(boss.getPosition().y / CHUNK_SIZE);
            if (loadedChunks.containsKey(makeKey(chunkX, chunkY))) {
                result.add(boss);
            }
        }
        return result;
    }

    public void addEnemyBoid(EnemyBoid enemy) {
        enemyBoids.add(enemy);
    }

    public void addBossBoid(BossBoid boss) {
        bossBoids.add(boss);
    }

    public void removeBossBoid(BossBoid boss) {
        bossBoids.remove(boss);
    }

    public void removeEnemyBoid(EnemyBoid enemy) {
        enemyBoids.remove(enemy);
    }

    public void setNormalBoidSpawnRate(double rate) {
        this.normalBoidSpawnRate = rate;
    }

    private boolean circleIntersectsRect(double cx, double cy, int radius, Rectangle rect) {
        double closestX = Math.max(rect.x, Math.min(cx, rect.x + rect.width));
        double closestY = Math.max(rect.y, Math.min(cy, rect.y + rect.height));
        double dx = cx - closestX;
        double dy = cy - closestY;
        return dx * dx + dy * dy < radius * radius;
    }

    private Chunk createChunk(int chunkX, int chunkY, Random rand, boolean generateNormalBoids) {
        Chunk chunk = new Chunk(chunkX, chunkY);

        int obstacleCount = 1 + rand.nextInt(3);
        for (int i = 0; i < obstacleCount; i++) {
            int width = 40 + rand.nextInt(50);
            int height = 40 + rand.nextInt(50);
            int x = chunkX * CHUNK_SIZE + rand.nextInt(CHUNK_SIZE - width);
            int y = chunkY * CHUNK_SIZE + rand.nextInt(CHUNK_SIZE - height);
            chunk.walls.add(new Wall(x + width / 2.0, y + height / 2.0, width, height));
        }

        if (generateNormalBoids) {
            int swarmCount = (int) (rand.nextInt(2) * normalBoidSpawnRate);
            for (int swarm = 0; swarm < swarmCount; swarm++) {
                int swarmSize = 5 + rand.nextInt(6);
                int centerX = chunkX * CHUNK_SIZE + 80 + rand.nextInt(CHUNK_SIZE - 160);
                int centerY = chunkY * CHUNK_SIZE + 80 + rand.nextInt(CHUNK_SIZE - 160);
                for (int i = 0; i < swarmSize; i++) {
                    double angle = rand.nextDouble() * Math.PI * 2;
                    double distance = 20 + rand.nextDouble() * 30;
                    int x = (int) Math.round(centerX + Math.cos(angle) * distance);
                    int y = (int) Math.round(centerY + Math.sin(angle) * distance);
                    
                    // Ensure boid doesn't spawn inside a wall
                    int attempts = 0;
                    while (collidesWithWall(x, y, 10) && attempts < 10) {
                        angle = rand.nextDouble() * Math.PI * 2;
                        distance = 20 + rand.nextDouble() * 30;
                        x = (int) Math.round(centerX + Math.cos(angle) * distance);
                        y = (int) Math.round(centerY + Math.sin(angle) * distance);
                        attempts++;
                    }
                    
                    String boidId = "normal-" + chunkX + "-" + chunkY + "-" + swarm + "-" + i;
                    if (removedNormalBoidIds.contains(boidId)) {
                        continue;
                    }
                    Vector velocity = new Vector(rand.nextDouble() * 2 - 1, rand.nextDouble() * 2 - 1);
                    velocity.normalize();
                    velocity.multiply(1.5);
                    normalBoids.add(new NormalBoid(boidId, x, y, 10, velocity, new Vector(0, 0)));
                }
            }
        }

        if (generateNormalBoids) {
            // Time-based enemy spawning with progressive difficulty
            long currentTime = System.currentTimeMillis();
            long gameTime = currentTime - gameStartTime;
            
            // Reduce spawn interval over time (minimum 2 seconds) - faster ramp up
            enemySpawnInterval = Math.max(2000, 12000 - (gameTime / 1000) * 100); // Reduce by 100ms per second
            
            if (currentTime - lastEnemySpawnTime >= enemySpawnInterval) {
                enemySwarmCount++;
                lastEnemySpawnTime = currentTime;
                
                // Every 7 waves, spawn a boss
                if (enemySwarmCount % 7 == 0) {
                    // Spawn boss
                    int centerX = chunkX * CHUNK_SIZE + 80 + rand.nextInt(CHUNK_SIZE - 160);
                    int centerY = chunkY * CHUNK_SIZE + 80 + rand.nextInt(CHUNK_SIZE - 160);
                    
                    // Ensure boss doesn't spawn inside a wall
                    int attempts = 0;
                    while (collidesWithWall(centerX, centerY, 25) && attempts < 10) {
                        centerX = chunkX * CHUNK_SIZE + 80 + rand.nextInt(CHUNK_SIZE - 160);
                        centerY = chunkY * CHUNK_SIZE + 80 + rand.nextInt(CHUNK_SIZE - 160);
                        attempts++;
                    }
                    
                    String bossId = "boss-" + chunkX + "-" + chunkY + "-" + enemySwarmCount;
                    
                    Vector bossVel = new Vector(rand.nextDouble() * 2 - 1, rand.nextDouble() * 2 - 1);
                    bossVel.normalize();
                    bossVel.multiply(1.5);
                    
                    bossSpawnCount++;
                    bossBoids.add(new BossBoid(bossId, centerX, centerY, bossSpawnCount, 25, bossVel, new Vector(0, 0)));
                    
                    // Spawn 15 enemy boids around the boss
                    for (int i = 0; i < 15; i++) {
                        double angle = rand.nextDouble() * Math.PI * 2;
                        double distance = 30 + rand.nextDouble() * 50;
                        int x = (int) Math.round(centerX + Math.cos(angle) * distance);
                        int y = (int) Math.round(centerY + Math.sin(angle) * distance);
                        
                        // Ensure enemy doesn't spawn inside a wall
                        int enemyAttempts = 0;
                        while (collidesWithWall(x, y, 10) && enemyAttempts < 10) {
                            angle = rand.nextDouble() * Math.PI * 2;
                            distance = 30 + rand.nextDouble() * 50;
                            x = (int) Math.round(centerX + Math.cos(angle) * distance);
                            y = (int) Math.round(centerY + Math.sin(angle) * distance);
                            enemyAttempts++;
                        }
                        
                        String boidId = "boss-enemy-" + chunkX + "-" + chunkY + "-" + enemySwarmCount + "-" + i;
                        if (removedEnemyBoidIds.contains(boidId)) {
                            continue;
                        }
                        Vector eVel = new Vector(rand.nextDouble() * 2 - 1, rand.nextDouble() * 2 - 1);
                        eVel.normalize();
                        eVel.multiply(2.0);
                        enemyBoids.add(new EnemyBoid(boidId, x, y, 10, eVel, new Vector(0, 0)));
                    }
                } else {
                    // Regular enemy swarm
                    if (enemySwarmCount % 5 == 0) {
                        enemySwarmMultiplier *= 1.2;
                        flankerSwarmCount++;
                        if (flankerSwarmCount % 4 == 0) {
                            flankerSwarmMultiplier *= 1.15;
                        }
                    }
                    int swarmSize = (int) ((10 + rand.nextInt(6)) * enemySwarmMultiplier);
                    int centerX = chunkX * CHUNK_SIZE + 80 + rand.nextInt(CHUNK_SIZE - 160);
                    int centerY = chunkY * CHUNK_SIZE + 80 + rand.nextInt(CHUNK_SIZE - 160);
                    for (int i = 0; i < swarmSize; i++) {
                        double angle = rand.nextDouble() * Math.PI * 2;
                        double distance = 20 + rand.nextDouble() * 40;
                        int x = (int) Math.round(centerX + Math.cos(angle) * distance);
                        int y = (int) Math.round(centerY + Math.sin(angle) * distance);
                        
                        // Ensure enemy doesn't spawn inside a wall
                        int attempts = 0;
                        while (collidesWithWall(x, y, 10) && attempts < 10) {
                            angle = rand.nextDouble() * Math.PI * 2;
                            distance = 20 + rand.nextDouble() * 40;
                            x = (int) Math.round(centerX + Math.cos(angle) * distance);
                            y = (int) Math.round(centerY + Math.sin(angle) * distance);
                            attempts++;
                        }
                        
                        String boidId = "enemy-" + chunkX + "-" + chunkY + "-" + enemySwarmCount + "-" + i;
                        if (removedEnemyBoidIds.contains(boidId)) {
                            continue;
                        }
                        Vector eVel = new Vector(rand.nextDouble() * 2 - 1, rand.nextDouble() * 2 - 1);
                        eVel.normalize();
                        eVel.multiply(2.0);
                        enemyBoids.add(new EnemyBoid(boidId, x, y, 10, eVel, new Vector(0, 0)));
                    }

                    int flankerCount = Math.max(10, (int) Math.round(10 * flankerSwarmMultiplier));
                    for (int i = 0; i < flankerCount; i++) {
                        double angle = rand.nextDouble() * Math.PI * 2;
                        double distance = 20 + rand.nextDouble() * 40;
                        int x = (int) Math.round(centerX + Math.cos(angle) * distance);
                        int y = (int) Math.round(centerY + Math.sin(angle) * distance);
                        
                        int attempts = 0;
                        while (collidesWithWall(x, y, 10) && attempts < 10) {
                            angle = rand.nextDouble() * Math.PI * 2;
                            distance = 20 + rand.nextDouble() * 40;
                            x = (int) Math.round(centerX + Math.cos(angle) * distance);
                            y = (int) Math.round(centerY + Math.sin(angle) * distance);
                            attempts++;
                        }
                        
                        String flankerId = "flanker-" + chunkX + "-" + chunkY + "-" + enemySwarmCount + "-" + i;
                        if (removedEnemyBoidIds.contains(flankerId)) {
                            continue;
                        }
                        Vector fVel = new Vector(rand.nextDouble() * 2 - 1, rand.nextDouble() * 2 - 1);
                        fVel.normalize();
                        fVel.multiply(5.0);
                        enemyBoids.add(new FlankerBoid(flankerId, x, y, 10, fVel, new Vector(0, 0)));
                    }
                }
            }
        }

        return chunk;
    }

    private String makeKey(int x, int y) {
        return x + ":" + y;
    }

    private static class Chunk {
        final int chunkX;
        final int chunkY;
        final List<Wall> walls = new ArrayList<>();

        Chunk(int chunkX, int chunkY) {
            this.chunkX = chunkX;
            this.chunkY = chunkY;
        }

        void draw(Graphics g, Camera camera) {
            int px = camera.getScreenX(chunkX * CHUNK_SIZE);
            int py = camera.getScreenY(chunkY * CHUNK_SIZE);
            g.setColor(new Color(28, 28, 32));
            g.fillRect(px, py, CHUNK_SIZE, CHUNK_SIZE);
            g.setColor(new Color(46, 46, 58));
            g.drawRect(px, py, CHUNK_SIZE, CHUNK_SIZE);

            for (Wall wall : walls) {
                wall.draw(g, camera);
            }
        }
    }
}
