package entities;

import java.util.ArrayList;
import math.Vector;
import world.MapGenerator;

public class Minion extends Boid {
    private double speedMultiplier = 1.0;

    public Minion(int xPos, int yPos, int radius, Vector velocity, Vector acceleration) {
        super(xPos, yPos, radius, velocity, acceleration);
        maxSpeed = 4.8;
        maxForce = 0.16;
    }

    public void update(Commander commander, ArrayList<Boid> swarm, MapGenerator mapGenerator) {
        Vector s = separate(swarm);
        Vector a = align(swarm);
        Vector c = cohere(swarm);
        Vector lead = arrive(commander.position, 140);

        s.multiply(3.5);
        a.multiply(0.6);
        c.multiply(0.3);
        lead.multiply(2.0);

        push(s);
        push(a);
        push(c);
        push(lead);
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
    }

    private Vector avoidWalls(MapGenerator mapGenerator) {
        Vector steer = new Vector(0, 0);
        
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

    public void setSpeedMultiplier(double multiplier) {
        this.speedMultiplier = multiplier;
        maxSpeed = 4.8 * multiplier;
    }
}
