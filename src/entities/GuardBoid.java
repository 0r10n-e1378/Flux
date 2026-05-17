/*
 * GuardBoid.java
 *
 * A stronger enemy type that follows boss behavior or the commander.
 * Guard boids still use flocking forces but may target nearby bosses.
 */
package entities;

import ai.Behavior;
import core.Camera;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.util.ArrayList;
import math.Vector;
import world.MapGenerator;

public class GuardBoid extends EnemyBoid {
    private int health = 3;

    public GuardBoid(String id, int xPos, int yPos, int radius, Vector velocity, Vector acceleration) {
        super(id, xPos, yPos, radius, velocity, acceleration);
        maxSpeed = 5.0;
        maxForce = 0.18;
    }

    public void takeDamage(int damage) {
        health -= damage;
    }

    public boolean isDead() {
        return health <= 0;
    }

    @Override
    public void update(ArrayList<EnemyBoid> flock, Commander commander, MapGenerator mapGenerator) {
        updateWithTarget(flock, commander.getPosition(), mapGenerator);
    }

    @Override
    public void updateWithTarget(ArrayList<EnemyBoid> flock, Vector target, MapGenerator mapGenerator) {
        ArrayList<Boid> flockAsBoids = new ArrayList<>(flock);

        Vector s = separate(flockAsBoids);
        Vector a = align(flockAsBoids);
        Vector c = cohere(flockAsBoids);
        Vector chase = seek(target);

        Behavior.scale(s, 3.5);
        Behavior.scale(a, 0.7);
        Behavior.scale(c, 0.4);
        Behavior.scale(chase, 1.5);

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
                velocity.y *= 0.5;
            } else if (!mapGenerator.collidesWithWall(testY.x, testY.y, radius)) {
                position = testY;
                velocity.x *= 0.5;
            } else {
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
                    velocity.multiply(0.1);
                }
            }
        }

        acceleration.multiply(0);
    }

    @Override
    public void draw(Graphics g, Camera camera) {
        int screenX = camera.getScreenX(position.x);
        int screenY = camera.getScreenY(position.y);
        double angle = velocity.magnitude() > 0 ? Math.atan2(velocity.y, velocity.x) : 0;

        Polygon guard = createGuardShape(screenX, screenY, radius, angle);
        g.setColor(new Color(150, 100, 255)); // Brighter purple
        g.fillPolygon(guard);
        g.setColor(Color.WHITE);
        g.drawPolygon(guard);
    }

    private Polygon createGuardShape(int x, int y, int radius, double angle) {
        int tipX = x + (int) (Math.cos(angle) * radius * 2);
        int tipY = y + (int) (Math.sin(angle) * radius * 2);
        int leftX = x + (int) (Math.cos(angle + Math.PI * 0.75) * radius);
        int leftY = y + (int) (Math.sin(angle + Math.PI * 0.75) * radius);
        int rightX = x + (int) (Math.cos(angle - Math.PI * 0.75) * radius);
        int rightY = y + (int) (Math.sin(angle - Math.PI * 0.75) * radius);
        int flatLeftX = tipX - (int) (Math.cos(angle) * radius * 0.6) + (int) (Math.cos(angle + Math.PI / 2) * radius * 0.3);
        int flatLeftY = tipY - (int) (Math.sin(angle) * radius * 0.6) + (int) (Math.sin(angle + Math.PI / 2) * radius * 0.3);
        int flatRightX = tipX - (int) (Math.cos(angle) * radius * 0.6) - (int) (Math.cos(angle + Math.PI / 2) * radius * 0.3);
        int flatRightY = tipY - (int) (Math.sin(angle) * radius * 0.6) - (int) (Math.sin(angle + Math.PI / 2) * radius * 0.3);

        return new Polygon(
            new int[] { tipX, flatLeftX, leftX, x, rightX, flatRightX },
            new int[] { tipY, flatLeftY, leftY, y, rightY, flatRightY },
            6
        );
    }
}