package world;

import entities.Commander;
import entities.EnemyBoid;
import entities.Minion;
import entities.NormalBoid;
import java.util.Random;
import math.Vector;

public class Spawner {
    private final Random rand = new Random();

    public Commander spawnCommander(double x, double y) {
        return new Commander((int) x, (int) y, 16, new Vector(0, 0), new Vector(0, 0));
    }

    public Minion spawnMinion(double x, double y) {
        return new Minion((int) x, (int) y, 10, new Vector(0, 0), new Vector(0, 0));
    }

    public Minion spawnMinion(double x, double y, Vector velocity) {
        return new Minion((int) x, (int) y, 10, new Vector(velocity.x, velocity.y), new Vector(0, 0));
    }

    public NormalBoid spawnNormalBoid(double x, double y) {
        Vector velocity = new Vector(rand.nextDouble() * 2 - 1, rand.nextDouble() * 2 - 1);
        velocity.normalize();
        velocity.multiply(1.5);
        String id = "spawned-" + System.nanoTime();
        return new NormalBoid(id, (int) x, (int) y, 10, velocity, new Vector(0, 0));
    }

    public EnemyBoid spawnEnemyBoid(String id, double x, double y) {
        Vector velocity = new Vector(rand.nextDouble() * 2 - 1, rand.nextDouble() * 2 - 1);
        velocity.normalize();
        velocity.multiply(2.0);
        return new EnemyBoid(id, (int) x, (int) y, 10, velocity, new Vector(0, 0));
    }
}
