package entities;

import core.Camera;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.util.ArrayList;
import math.Vector;
import world.MapGenerator;

public class NormalBoid extends Boid {
    private final String id;

    public NormalBoid(String id, int xPos, int yPos, int radius, Vector velocity, Vector acceleration) {
        super(xPos, yPos, radius, velocity, acceleration);
        this.id = id;
        maxSpeed = 3.0;
        maxForce = 0.08;
    }

    public String getId() {
        return id;
    }

    public void update(ArrayList<Boid> flock, MapGenerator mapGenerator) {
        Vector s = separate(flock);
        Vector a = align(flock);
        Vector c = cohere(flock);

        s.multiply(3.2);
        a.multiply(0.65);
        c.multiply(0.35);

        push(s);
        push(a);
        push(c);
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
            steer.recalculate(maxForce * 0.8);
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
        g.setColor(new Color(220, 190, 75));
        g.fillPolygon(arrow);
        g.setColor(Color.BLACK);
        g.drawPolygon(arrow);
    }
}
