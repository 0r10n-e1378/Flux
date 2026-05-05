package entities;

public class Trap extends Entity{

	private int damage;
	
	public Trap (double xPos, double yPos, int radius, int damage) {
		super(xPos, yPos, radius);
		this.damage = damage;
	}
	
	public int getDamage () {
		return damage;
	}
	
}
