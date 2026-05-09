package entities;

import core.Camera;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.util.ArrayList;
import math.Vector;
import world.MapGenerator;

public class BossBoid extends Boid {
    private final String id;
    private int health = 50;
    private long lastSpawnTime = 0;
    private final long spawnInterval = 3000; // 3 seconds

    public BossBoid(String id, int xPos, int yPos, int radius, Vector velocity, Vector acceleration) {
        super(xPos, yPos, radius, velocity, acceleration);
        this.id = id;
        maxSpeed = 3.0; // Slower than regular enemies
        maxForce = 0.15;
    }

    public String getId() {
        return id;
    }

    public int getHealth() {
        return health;
    }

    public void takeDamage(int damage) {
        health -= damage;
    }

    public boolean isDead() {
        return health <= 0;
    }

    public void update(ArrayList<EnemyBoid> flock, Commander commander, MapGenerator mapGenerator) {
        ArrayList<Boid> flockAsBoids = new ArrayList<>(flock);

        Vector s = separate(flockAsBoids);
        Vector a = align(flockAsBoids);
        Vector c = cohere(flockAsBoids);
        Vector chase = seek(commander.getPosition());

        s.multiply(3.5);
        a.multiply(0.7);
        c.multiply(0.4);
        chase.multiply(1.5); // Less aggressive chase

        push(s);
        push(a);
        push(c);
        push(chase);
        push(avoidWalls(mapGenerator));

        velocity.add(acceleration);
        velocity.recalculate(maxSpeed);

        Vector nextPosition = new Vector(position.x + velocity.x, position.y + velocity.y);
        if (!mapGenerator.collidesWithWall(nextPosition.x, nextPosition.y, radius)) {
            position = nextPosition;
        } else {
            Vector testX = new Vector(position.x + velocity.x, position.y);
            Vector testY = new Vector(position.x, position.y + velocity.y);
            if (!mapGenerator.collidesWithWall(testX.x, testX.y, radius)) {
                position = testX;
            } else if (!mapGenerator.collidesWithWall(testY.x, testY.y, radius)) {
                position = testY;
            } else {
                velocity.multiply(0);
            }
        }

        acceleration.multiply(0);

        // Spawn enemy boids periodically
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSpawnTime >= spawnInterval) {
            spawnEnemies(mapGenerator);
            lastSpawnTime = currentTime;
        }
    }

    private void spawnEnemies(MapGenerator mapGenerator) {
        // Spawn 2-4 enemy boids around the boss
        int spawnCount = 2 + (int)(Math.random() * 3);
        for (int i = 0; i < spawnCount; i++) {
            double angle = Math.random() * Math.PI * 2;
            double distance = 50 + Math.random() * 30;
            int x = (int)(position.x + Math.cos(angle) * distance);
            int y = (int)(position.y + Math.sin(angle) * distance);

            Vector velocity = new Vector(Math.random() * 2 - 1, Math.random() * 2 - 1);
            velocity.normalize();
            velocity.multiply(2.0);

            mapGenerator.addEnemyBoid(new EnemyBoid("boss-spawn-" + System.currentTimeMillis() + "-" + i,
                                                   x, y, 10, velocity, new Vector(0, 0)));
        }
    }

    private Vector avoidWalls(MapGenerator mapGenerator) {
        Vector steer = new Vector(0, 0);

        // Check if currently in a wall - if so, provide strong escape force
        if (mapGenerator.collidesWithWall(position.x, position.y, radius)) {
            int[][] directions = {
                { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 },
                { -1, -1 }, { 1, -1 }, { -1, 1 }, { 1, 1 }
            };
            for (int[] dir : directions) {
                Vector testPos = new Vector(position.x + dir[0] * 20, position.y + dir[1] * 20);
                if (!mapGenerator.collidesWithWall(testPos.x, testPos.y, radius)) {
                    steer.add(new Vector(dir[0], dir[1]));
                }
            }
            if (steer.magnitude() > 0) {
                steer.normalize();
                steer.multiply(maxSpeed * 2);
                steer.subtract(velocity);
                steer.recalculate(maxForce * 3.0);
                return steer;
            }
        }

        // Normal wall avoidance with lookahead
        int probeDistance = radius + 30;
        int[][] directions = {
            { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 },
            { -1, -1 }, { 1, -1 }, { -1, 1 }, { 1, 1 }
        };

        for (int[] dir : directions) {
            Vector probe = new Vector(position.x + dir[0] * probeDistance, position.y + dir[1] * probeDistance);
            if (mapGenerator.collidesWithWall(probe.x, probe.y, radius)) {
                steer.add(new Vector(-dir[0], -dir[1]));
            }
        }

        if (steer.magnitude() > 0) {
            steer.normalize();
            steer.multiply(maxSpeed);
            steer.subtract(velocity);
            steer.recalculate(maxForce * 2.0);
            return steer;
        }
        return new Vector(0, 0);
    }

    @Override
    public void draw(Graphics g, Camera camera) {
        int screenX = camera.getScreenX(position.x);
        int screenY = camera.getScreenY(position.y);
        double angle = velocity.magnitude() > 0 ? Math.atan2(velocity.y, velocity.x) : 0;

        Polygon arrow = createArrowShape(screenX, screenY, radius, angle);
        g.setColor(new Color(150, 0, 150)); // Purple for boss
        g.fillPolygon(arrow);
        g.setColor(Color.WHITE);
        g.drawPolygon(arrow);

        // Draw health bar
        int barWidth = radius * 3;
        int barHeight = 4;
        int barX = screenX - barWidth / 2;
        int barY = screenY - radius - 10;

        g.setColor(Color.DARK_GRAY);
        g.fillRect(barX, barY, barWidth, barHeight);
        g.setColor(Color.RED);
        g.fillRect(barX, barY, (int)(barWidth * health / 50.0), barHeight);
        g.setColor(Color.WHITE);
        g.drawRect(barX, barY, barWidth, barHeight);
    }
}