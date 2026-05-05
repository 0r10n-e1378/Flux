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
            return seek(sum); // Cohesion is just seeking the center of mass
        }
        return new Vector(0,0);
    }

    // Helper to turn a "desired" vector into a steering force
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
	
}
