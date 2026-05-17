/*
 * SteeringSolution.java
 *
 * Educational implementation of the core boid steering rules.
 * This is where separation, alignment, and cohesion are actually computed.
 */
package ai;

import entities.Boid;
import java.util.ArrayList;
import math.Vector;

public class SteeringSolution {

    // Core steering rule: keep distance from nearby boids to avoid crowding.
    public static Vector separate(Boid self, ArrayList<Boid> neighbors) {
        Vector steer = new Vector(0, 0);
        double separation = self.getRadius() * 4.0;
        int count = 0;

        for (Boid other : neighbors) {
            if (other == self) {
                continue;
            }
            double d = Vector.distance(self.getPosition(), other.getPosition());
            if (d > 0 && d < separation) {
                Vector diff = new Vector(self.getPosition().x, self.getPosition().y);
                diff.subtract(other.getPosition());
                diff.normalize();
                diff.divide(d * d);
                steer.add(diff);
                count++;
            }
        }
        return processSteer(steer, count, self);
    }

    // Core steering rule: align velocity with nearby boids so the group moves together.
    public static Vector align(Boid self, ArrayList<Boid> neighbors) {
        Vector sum = new Vector(0, 0);
        int count = 0;
        for (Boid other : neighbors) {
            if (other == self) {
                continue;
            }
            sum.add(other.getVelocity());
            count++;
        }
        return processSteer(sum, count, self);
    }

    // Core steering rule: move toward the average position of nearby boids.
    public static Vector cohere(Boid self, ArrayList<Boid> neighbors) {
        Vector sum = new Vector(0, 0);
        int count = 0;
        for (Boid other : neighbors) {
            if (other == self) {
                continue;
            }
            sum.add(other.getPosition());
            count++;
        }
        if (count > 0) {
            sum.divide(count);
            return seek(self, sum);
        }
        return new Vector(0, 0);
    }

    public static Vector seek(Boid self, Vector target) {
        Vector desired = new Vector(target.x, target.y);
        desired.subtract(self.getPosition());
        desired.normalize();
        desired.multiply(self.getMaxSpeed());
        Vector steer = new Vector(desired.x, desired.y);
        steer.subtract(self.getVelocity());
        steer.recalculate(self.getMaxForce());
        return steer;
    }

    private static Vector processSteer(Vector target, int count, Boid self) {
        if (count > 0) {
            target.divide(count);
            target.normalize();
            target.multiply(self.getMaxSpeed());
            Vector steer = new Vector(target.x, target.y);
            steer.subtract(self.getVelocity());
            steer.recalculate(self.getMaxForce());
            return steer;
        }
        return new Vector(0, 0);
    }
}
