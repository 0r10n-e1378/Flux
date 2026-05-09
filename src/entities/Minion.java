package entities;

import java.util.ArrayList;
import math.Vector;
import world.MapGenerator;

public class Minion extends Boid {

    public Minion(int xPos, int yPos, int radius, Vector velocity, Vector acceleration) {
        super(xPos, yPos, radius, velocity, acceleration);
        maxSpeed = 4.8;
        maxForce = 0.16;
    }

    public void update(Commander commander, ArrayList<Boid> swarm, MapGenerator mapGenerator) {
        Vector s = separate(swarm);
        Vector a = align(swarm);
        Vector c = cohere(swarm);
        Vector lead = arrive(commander.position, 140);

        s.multiply(3.5);
        a.multiply(0.6);
        c.multiply(0.3);
        lead.multiply(2.0); // Still follow the commander but allow more spread

        push(s);
        push(a);
        push(c);
        push(lead);

        velocity.add(acceleration);
        velocity.recalculate(maxSpeed);

        Vector nextPosition = new Vector(position.x + velocity.x, position.y + velocity.y);
        if (!mapGenerator.collidesWithWall(nextPosition.x, nextPosition.y, radius)) {
            position = nextPosition;
        } else {
            Vector testX = new Vector(position.x + velocity.x, position.y);
            Vector testY = new Vector(position.x, position.y + velocity.y);
            if (!mapGenerator.collidesWithWall(testX.x, testX.y, radius)) {
                position = testX;
            } else if (!mapGenerator.collidesWithWall(testY.x, testY.y, radius)) {
                position = testY;
            } else {
                velocity.multiply(0);
            }
        }

        acceleration.multiply(0);
    }
}
