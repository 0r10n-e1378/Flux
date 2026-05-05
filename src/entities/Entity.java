package entities;

import math.Vector;

public class Entity {
	
	protected int radius;
	protected Vector position;
	
	public Entity (double x, double y, int radius) {
		position = new Vector(x, y);
		this.radius = radius;	
	}
	
	public void draw () {
	}
	
}