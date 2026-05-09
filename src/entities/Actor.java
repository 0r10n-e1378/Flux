package entities;

import math.Vector;

public class Actor extends Entity{

	protected double maxSpeed, maxForce;
	protected Vector velocity, acceleration;
	
	public Actor (double x, double y, int radius, Vector velocity, Vector acceleration) {
		super(x, y, radius);
		maxSpeed = 4.0;
		maxForce = 0.1;
		this.velocity = velocity;
		this.acceleration = acceleration;
	}
	
	public void update () {
		velocity.add(acceleration);
		velocity.recalculate(maxSpeed);
		position.add(velocity);
		acceleration.multiply(0);
	}
	
	public void push (Vector v) {
		acceleration.add(v);
	}

    public Vector getVelocity() {
        return velocity;
    }

    public Vector getAcceleration() {
        return acceleration;
    }
}
