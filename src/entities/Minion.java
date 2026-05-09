package entities;

import math.Vector;
import java.util.ArrayList;

public class Minion extends Boid{

	public Minion (int xPos, int yPos, int radius, Vector velocity, Vector acceleration) {
		super(xPos, yPos, radius, velocity, acceleration);
	}
	
	public void update(Commander commander, ArrayList<Boid> swarm) {
        Vector s = separate(swarm);
        Vector a = align(swarm);
        Vector c = cohere(swarm);
        Vector lead = seek(commander.position);

        s.multiply(1.5);
        a.multiply(1.0);
        c.multiply(1.0);
        lead.multiply(5.0); // Extreme tendency to follow commander

        push(s);
        push(a);
        push(c); 
        push(lead);
        super.update();
    }
	
}
