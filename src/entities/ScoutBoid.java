/*
 * ScoutBoid.java
 *
 * Fast, aggressive enemies that charge the commander.
 * Scouts now also use the same flocking forces as other enemies.
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

public class ScoutBoid extends EnemyBoid {

    public ScoutBoid(String id, int xPos, int yPos, int radius, Vector velocity, Vector acceleration) {
        super(id, xPos, yPos, radius, velocity, acceleration);
        maxSpeed = 12.5; // 2.5 times normal enemy speed
        maxForce = 0.45;
    }

    @Override
    public void update(ArrayList<EnemyBoid> flock, Commander commander, MapGenerator mapGenerator) {
        ArrayList<Boid> flockAsBoids = new ArrayList<>(flock);

        Vector s = separate(flockAsBoids);
        Vector a = align(flockAsBoids);
        Vector c = cohere(flockAsBoids);
        Vector chase = seek(commander.getPosition());

        Behavior.scale(s, 3.0);
        Behavior.scale(a, 0.7);
        Behavior.scale(c, 0.4);
        Behavior.scale(chase, 3.0); // Strong chase priority

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

        // Draw rectangular tail
        int tailLength = radius + 8;
        int tailWidth = Math.max(2, radius / 2);
        int rearX = screenX - (int) (Math.cos(angle) * radius * 0.8);
        int rearY = screenY - (int) (Math.sin(angle) * radius * 0.8);

        int topX = rearX + (int) (Math.cos(angle + Math.PI / 2) * tailWidth);
        int topY = rearY + (int) (Math.sin(angle + Math.PI / 2) * tailWidth);
        int bottomX = rearX - (int) (Math.cos(angle + Math.PI / 2) * tailWidth);
        int bottomY = rearY - (int) (Math.sin(angle + Math.PI / 2) * tailWidth);
        int topBackX = topX - (int) (Math.cos(angle) * tailLength);
        int topBackY = topY - (int) (Math.sin(angle) * tailLength);
        int bottomBackX = bottomX - (int) (Math.cos(angle) * tailLength);
        int bottomBackY = bottomY - (int) (Math.sin(angle) * tailLength);

        Polygon tail = new Polygon(
            new int[] { topX, bottomX, bottomBackX, topBackX },
            new int[] { topY, bottomY, bottomBackY, topBackY },
            4
        );

        Color bodyColor = new Color(255, 100, 100); // Bright red for scouts
        g.setColor(bodyColor);
        g.fillPolygon(tail);
        Polygon arrow = createArrowShape(screenX, screenY, radius, angle);
        g.fillPolygon(arrow);
        g.setColor(Color.WHITE);
        g.drawPolygon(arrow);
        g.drawPolygon(tail);
    }
}
