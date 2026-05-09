package entities;

import core.Camera;
import java.awt.Graphics;
import math.Vector;

public class Entity {
    protected int radius;
    protected Vector position;

    public Entity(double x, double y, int radius) {
        position = new Vector(x, y);
        this.radius = radius;
    }

    public double getX() {
        return position.x;
    }

    public double getY() {
        return position.y;
    }

    public Vector getPosition() {
        return position;
    }

    public int getRadius() {
        return radius;
    }

    public void update() {
        // Override in subclasses when needed.
    }

    public void draw(Graphics g, Camera camera) {
        // Override in subclasses.
    }
}
