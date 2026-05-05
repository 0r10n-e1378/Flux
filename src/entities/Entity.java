package entities;

public class Entity {
	
	private int xPos, yPos, radius;
	
	public Entity (int xPos, int yPos, int radius) {
		this.xPos = xPos;
		this.yPos = yPos;
		this.radius = radius;	
	}
	
	public int getX () {
		return xPos;
	}
	
	public int getY () {
		return yPos;
	}
	
	public int getRadius () {
		return radius;
	}
	
	public void draw () {
	}
	
}