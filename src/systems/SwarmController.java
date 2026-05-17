/*
 * SwarmController.java
 *
 * Collects nearby boids into a neighbor list for flocking.
 * It builds a temporary grid every frame for the query.
 */
package systems;

import entities.Boid;
import entities.EnemyBoid;
import entities.Minion;
import entities.NormalBoid;
import java.util.ArrayList;
import java.util.List;
import math.Vector;

public class SwarmController {

    public static ArrayList<Boid> getNeighbors(Vector position, double searchRadius,
                                               ArrayList<NormalBoid> normals,
                                               ArrayList<EnemyBoid> enemies,
                                               List<Minion> minions) {
        Grid grid = new Grid((int) searchRadius);
        for (NormalBoid normal : normals) {
            grid.add(normal);
        }
        for (EnemyBoid enemy : enemies) {
            grid.add(enemy);
        }
        for (Minion minion : minions) {
            grid.add(minion);
        }
        return grid.query(position, searchRadius);
    }
}
