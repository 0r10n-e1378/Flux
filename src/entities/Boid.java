package entities;

import ai.Steering;
import core.Camera;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.util.ArrayList;
import math.Vector;

public class Boid extends Actor{

	public Boid (int x, int y, int radius, Vector velocity, Vector acceleration) {
		super(x, y, radius, velocity, acceleration);
	}
	
	protected Vector separate(ArrayList<Boid> neighbors) {
        return Steering.separate(this, neighbors);
    }

    protected Vector align(ArrayList<Boid> neighbors) {
        return Steering.align(this, neighbors);
    }

    protected Vector cohere(ArrayList<Boid> neighbors) {
        return Steering.cohere(this, neighbors);
    }

    // Helper to turn a desired vector into a steering force
    protected Vector processSteer(Vector target, int count) {
        if (count > 0) {
            target.divide(count);
            target.normalize();
            target.multiply(maxSpeed);
            Vector steer = new Vector(target.x, target.y);
            steer.subtract(velocity);
            steer.recalculate(maxForce);
            return steer;
        }
        return new Vector(0, 0);
    }

    protected Vector seek(Vector target) {
        Vector desired = new Vector(target.x, target.y);
        desired.subtract(this.position);
        desired.normalize();
        desired.multiply(maxSpeed);
        Vector steer = new Vector(desired.x, desired.y);
        steer.subtract(this.velocity);
        steer.recalculate(maxForce);
        return steer;
    }

    protected Vector arrive(Vector target, double slowingRadius) {
        Vector desired = new Vector(target.x, target.y);
        desired.subtract(this.position);
        double distance = desired.magnitude();
        if (distance == 0) {
            return new Vector(0, 0);
        }

        desired.normalize();
        if (distance < slowingRadius) {
            desired.multiply(maxSpeed * (distance / slowingRadius));
        } else {
            desired.multiply(maxSpeed);
        }

        Vector steer = new Vector(desired.x, desired.y);
        steer.subtract(this.velocity);
        steer.recalculate(maxForce);
        return steer;
    }
    
    protected Vector avoidBoundaries(int screenWidth, int screenHeight, double margin) {
        Vector desired = null;

        // Check X boundaries (Left and Right walls)
        if (this.position.x < margin) {
            // Too close to left wall, desire to move right
            desired = new Vector(maxSpeed, this.velocity.y);
        } else if (this.position.x > screenWidth - margin) {
            // Too close to right wall, desire to move left
            desired = new Vector(-maxSpeed, this.velocity.y);
        }

        // Check Y boundaries (Top and Bottom walls)
        if (this.position.y < margin) {
            // Too close to top wall, desire to move down
            if (desired != null) {
                desired.y = maxSpeed; // Combine with X avoidance if in a corner
            } else {
                desired = new Vector(this.velocity.x, maxSpeed);
            }
        } else if (this.position.y > screenHeight - margin) {
            // Too close to bottom wall, desire to move up
            if (desired != null) {
                desired.y = -maxSpeed; // Combine with X avoidance if in a corner
            } else {
                desired = new Vector(this.velocity.x, -maxSpeed);
            }
        }

        // Calculate steering force if we are near a wall
        if (desired != null) {
            desired.normalize();
            desired.multiply(maxSpeed);
            
            // Steering = Desired - Velocity
            Vector steer = new Vector(desired.x, desired.y);
            steer.subtract(this.velocity);
            steer.recalculate(maxForce); // Limits the force maximum force value
            return steer;
        }

        // Return a zero vector if safe
        return new Vector(0, 0); 
    }

    @Override
    public void draw(Graphics g, Camera camera) {
        int screenX = camera.getScreenX(position.x);
        int screenY = camera.getScreenY(position.y);
        double angle = velocity.magnitude() > 0 ? Math.atan2(velocity.y, velocity.x) : 0;

        Polygon arrow = createArrowShape(screenX, screenY, radius, angle);
        g.setColor(new Color(80, 160, 255));
        g.fillPolygon(arrow);
        g.setColor(Color.WHITE);
        g.drawPolygon(arrow);
    }

    protected Polygon createArrowShape(int x, int y, int radius, double angle) {
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
}