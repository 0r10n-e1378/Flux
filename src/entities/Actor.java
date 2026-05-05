package entities;

public class Actor extends Entity{

	private int acceleration, health, maxSpeed;
	
	public Actor (int xPos, int yPos, int radius, int acceleration, int health, int maxSpeed) {
		super(xPos, yPos, radius);
		this.acceleration = acceleration;
		this.health = health;
		this.maxSpeed = maxSpeed;
	}
	
	public int getAcceleration () {
		return acceleration;
	}
	
	public int getHealth () {
		return health;
	}
	
	public int getMaxSpeed () {
		return maxSpeed;
	}
	
}
