/*
 * Steering.java
 *
 * Public steering API for boids.
 * This class forwards separation, alignment, and cohesion requests
 * to the educational implementation in SteeringSolution.
 */
package ai;

import entities.Boid;
import java.util.ArrayList;
import math.Vector;

public class Steering {

    public static Vector separate(Boid self, ArrayList<Boid> neighbors) {

        return SteeringSolution.separate(self, neighbors);

        // Vector steer = new Vector(0, 0);
        // int count = 0;
        // return processSteer(steer, count, self);
    }

    public static Vector align(Boid self, ArrayList<Boid> neighbors) {

        return SteeringSolution.align(self, neighbors);

        // Vector sum = new Vector(0, 0);
        // int count = 0;
        // return processSteer(sum, count, self);
    }

    public static Vector cohere(Boid self, ArrayList<Boid> neighbors) {

        return SteeringSolution.cohere(self, neighbors);

        // return new Vector(0, 0);
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
