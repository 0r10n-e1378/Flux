package entities;

import math.Vector;
import java.util.ArrayList;

public class Boid extends Actor{

	public Boid (int x, int y, int radius, Vector velocity, Vector acceleration) {
		super(x, y, radius, velocity, acceleration);
	}
	
	protected Vector separate(ArrayList<Boid> neighbors) {
        Vector steer = new Vector(0, 0);
        double separation = radius * 2.5; 
        int count = 0;

        for (Boid other : neighbors) {
            double d = Vector.distance(position, other.position);
            if ((d > 0) && (d < separation)) {
                Vector diff = new Vector(position.x, position.y);
                diff.subtract(other.position);
                diff.normalize();
                diff.divide(d); // Weight by distance
                steer.add(diff);
                count++;
            }
        }
        return processSteer(steer, count);
    }

    protected Vector align(ArrayList<Boid> neighbors) {
        Vector sum = new Vector(0, 0);
        int count = 0;
        for (Boid other : neighbors) {
            sum.add(other.velocity);
            count++;
        }
        return processSteer(sum, count);
    }

    protected Vector cohere(ArrayList<Boid> neighbors) {
        Vector sum = new Vector(0, 0);
        int count = 0;
        for (Boid other : neighbors) {
            sum.add(other.position);
            count++;
        }
        if (count > 0) {
            sum.divide(count);
            return seek(sum); // Cohesion is seeking the center of mass
        }
        return new Vector(0,0);
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
	
}