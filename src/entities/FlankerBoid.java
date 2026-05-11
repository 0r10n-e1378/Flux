package entities;

import core.Camera;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.util.ArrayList;
import math.Vector;
import world.MapGenerator;

public class FlankerBoid extends EnemyBoid {
    private final int sidePreference;

    public FlankerBoid(String id, int xPos, int yPos, int radius, Vector velocity, Vector acceleration) {
        super(id, xPos, yPos, radius, velocity, acceleration);
        maxSpeed = 10.0;
        maxForce = 0.32;
        sidePreference = Math.random() < 0.5 ? -1 : 1;
    }

    @Override
    public void update(ArrayList<EnemyBoid> flock, Commander commander, MapGenerator mapGenerator) {
        ArrayList<Boid> flockAsBoids = new ArrayList<>(flock);

        Vector s = separate(flockAsBoids);
        Vector a = align(flockAsBoids);
        Vector c = cohere(flockAsBoids);
        Vector target = chooseFlankTarget(commander);
        Vector chase = seek(target);

        s.multiply(3.2);
        a.multiply(0.8);
        c.multiply(0.35);
        chase.multiply(3.0);

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

    private Vector chooseFlankTarget(Commander commander) {
        Vector commanderPos = commander.getPosition();
        Vector commanderVel = commander.getVelocity();
        double sideDistance = 90;
        double forwardOffset = -40;

        if (commanderVel.magnitude() < 0.7) {
            return commanderPos;
        }

        Vector forward = new Vector(commanderVel.x, commanderVel.y);
        forward.normalize();
        Vector perp = new Vector(-forward.y, forward.x);

        Vector toSelf = new Vector(position.x - commanderPos.x, position.y - commanderPos.y);
        double sideDot = toSelf.x * perp.x + toSelf.y * perp.y;
        int approachSide = sideDot == 0 ? sidePreference : (sideDot > 0 ? 1 : -1);

        Vector flankPoint = new Vector(commanderPos.x + perp.x * sideDistance * approachSide,
                                       commanderPos.y + perp.y * sideDistance * approachSide);
        flankPoint.add(new Vector(forward.x * forwardOffset, forward.y * forwardOffset));

        double distanceToFlank = Vector.distance(position, flankPoint);
        if (distanceToFlank < 50) {
            Vector sideAttackPoint = new Vector(commanderPos.x + perp.x * 40 * approachSide,
                                                commanderPos.y + perp.y * 40 * approachSide);
            return sideAttackPoint;
        }

        return flankPoint;
    }

    @Override
    public void draw(Graphics g, Camera camera) {
        int screenX = camera.getScreenX(position.x);
        int screenY = camera.getScreenY(position.y);
        double angle = velocity.magnitude() > 0 ? Math.atan2(velocity.y, velocity.x) : 0;

        int tailLength = radius + 6;
        int tailHalf = Math.max(3, radius / 3);
        int rearX = screenX - (int) (Math.cos(angle) * radius * 0.8);
        int rearY = screenY - (int) (Math.sin(angle) * radius * 0.8);

        int leftX = rearX + (int) (Math.cos(angle + Math.PI / 2) * tailHalf);
        int leftY = rearY + (int) (Math.sin(angle + Math.PI / 2) * tailHalf);
        int rightX = rearX + (int) (Math.cos(angle - Math.PI / 2) * tailHalf);
        int rightY = rearY + (int) (Math.sin(angle - Math.PI / 2) * tailHalf);
        int leftBackX = leftX - (int) (Math.cos(angle) * tailLength);
        int leftBackY = leftY - (int) (Math.sin(angle) * tailLength);
        int rightBackX = rightX - (int) (Math.cos(angle) * tailLength);
        int rightBackY = rightY - (int) (Math.sin(angle) * tailLength);

        Polygon tail = new Polygon(
            new int[] { leftX, rightX, rightBackX, leftBackX },
            new int[] { leftY, rightY, rightBackY, leftBackY },
            4
        );

        Color bodyColor = new Color(220, 120, 40);
        g.setColor(bodyColor);
        g.fillPolygon(tail);
        Polygon arrow = createArrowShape(screenX, screenY, radius, angle);
        g.fillPolygon(arrow);
        g.setColor(Color.WHITE);
        g.drawPolygon(arrow);
        g.drawPolygon(tail);
    }
}
