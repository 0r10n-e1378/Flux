package entities;

import core.Camera;
import core.Input;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import math.Vector;
import world.MapGenerator;

public class Commander extends Actor {
    private Color fillColor = new Color(100, 220, 100);
    private Color outlineColor = Color.WHITE;
    private int health = 100;
    private double speedMultiplier = 1.0;

    public Commander(int xPos, int yPos, int radius, Vector velocity, Vector acceleration) {
        super(xPos, yPos, radius, velocity, acceleration);
        maxSpeed = 5.0;
        maxForce = 0.2;
    }

    public void update(Input input, MapGenerator mapGenerator) {
        Vector direction = input.getDirection();
        if (direction.magnitude() > 0) {
            direction.multiply(maxForce * 2.0);
            push(direction);
        }

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

    public int getHealth() {
        return health;
    }

    public void takeDamage(int damage) {
        health -= damage;
        if (health < 0) {
            health = 0;
        }
    }

    public void resetHealth() {
        health = 100;
    }

    @Override
    public void draw(Graphics g, Camera camera) {
        int screenX = camera.getScreenX(position.x);
        int screenY = camera.getScreenY(position.y);
        double angle = velocity.magnitude() > 0 ? Math.atan2(velocity.y, velocity.x) : 0;
        Polygon arrow = createArrowShape(screenX, screenY, radius, angle);

        g.setColor(fillColor);
        g.fillPolygon(arrow);
        g.setColor(outlineColor);
        g.drawPolygon(arrow);
    }

    private Polygon createArrowShape(int x, int y, int radius, double angle) {
        int tipX = x + (int) (Math.cos(angle) * radius * 2);
        int tipY = y + (int) (Math.sin(angle) * radius * 2);
        int leftX = x + (int) (Math.cos(angle + Math.PI * 0.75) * radius);
        int leftY = y + (int) (Math.sin(angle + Math.PI * 0.75) * radius);
        int rightX = x + (int) (Math.cos(angle - Math.PI * 0.75) * radius);
        int rightY = y + (int) (Math.sin(angle - Math.PI * 0.75) * radius);

        return new Polygon(
            new int[] { tipX, leftX, x, rightX },
            new int[] { tipY, leftY, y, rightY },
            4
        );
    }

    public void setSpeedMultiplier(double multiplier) {
        this.speedMultiplier = multiplier;
        maxSpeed = 5.0 * multiplier;
    }
}
