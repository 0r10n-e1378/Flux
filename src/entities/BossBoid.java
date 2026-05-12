package entities;

import ai.Behavior;
import core.Camera;
import core.Formation;
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
    private long spawnInterval;
    private final Formation formation;

    public BossBoid(String id, int xPos, int yPos, int bossLevel, int radius, Vector velocity, Vector acceleration) {
        super(xPos, yPos, radius, velocity, acceleration);
        this.id = id;
        maxSpeed = 5.0; // Match commander's speed
        maxForce = 0.3; // Higher force for better responsiveness
        this.spawnInterval = Math.max(1000, 3000 - (bossLevel - 1) * 300);
        // Assign random formation (not NORMAL)
        Formation[] formations = {Formation.HEX_SHIELD, Formation.ARROWHEAD, Formation.PHALANX};
        this.formation = formations[(int)(Math.random() * formations.length)];
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

    public Formation getFormation() {
        return formation;
    }

    public void update(ArrayList<EnemyBoid> flock, Commander commander, MapGenerator mapGenerator) {
        // Boss only targets commander, not affected by flock
        Vector chase = seek(commander.getPosition());
        Behavior.scale(chase, 2.0); // Strong chase priority

        // Only avoid walls if they're directly in the way
        Vector wallAvoid = avoidWalls(mapGenerator);
        Behavior.scale(wallAvoid, 0.5); // Reduce wall avoidance strength

        push(chase);
        push(wallAvoid);

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
                velocity.y *= 0.5; // Reduce perpendicular velocity
            } else if (!mapGenerator.collidesWithWall(testY.x, testY.y, radius)) {
                position = testY;
                velocity.x *= 0.5; // Reduce perpendicular velocity
            } else {
                // If completely stuck, apply strong escape force
                Vector escapeForce = new Vector(0, 0);
                int[][] directions = {
                    { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 },
                    { -1, -1 }, { 1, -1 }, { -1, 1 }, { 1, 1 }
                };
                for (int[] dir : directions) {
                    Vector testPos = new Vector(position.x + dir[0] * 30, position.y + dir[1] * 30);
                    if (!mapGenerator.collidesWithWall(testPos.x, testPos.y, radius)) {
                        escapeForce.add(new Vector(dir[0], dir[1]));
                    }
                }
                if (escapeForce.magnitude() > 0) {
                    escapeForce.normalize();
                    escapeForce.multiply(maxSpeed * 0.5);
                    velocity = escapeForce;
                    position = new Vector(position.x + velocity.x, position.y + velocity.y);
                } else {
                    velocity.multiply(0.1); // Slow down if no escape found
                }
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
        // Don't spawn additional enemies - guards are already spawned on boss creation
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

        // Minimal wall avoidance - only when very close
        int probeDistance = radius + 10; // Much shorter probe distance
        int[][] directions = {
            { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 }
        }; // Only cardinal directions, no diagonals

        for (int[] dir : directions) {
            Vector probe = new Vector(position.x + dir[0] * probeDistance, position.y + dir[1] * probeDistance);
            if (mapGenerator.collidesWithWall(probe.x, probe.y, radius)) {
                steer.add(new Vector(-dir[0], -dir[1]));
            }
        }

        if (steer.magnitude() > 0) {
            steer.normalize();
            steer.multiply(maxSpeed * 0.3); // Much lower speed multiplier
            steer.subtract(velocity);
            steer.recalculate(maxForce * 0.5); // Much lower force multiplier
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