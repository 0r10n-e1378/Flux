package systems;

import entities.Boid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import math.Vector;

public class Grid {
    private final int cellSize;
    private final Map<Long, ArrayList<Boid>> cells = new HashMap<>();

    public Grid(int cellSize) {
        this.cellSize = Math.max(1, cellSize);
    }

    public void add(Boid boid) {
        long key = cellKey(cellX(boid.getPosition().x), cellY(boid.getPosition().y));
        cells.computeIfAbsent(key, k -> new ArrayList<>()).add(boid);
    }

    public ArrayList<Boid> query(Vector position, double radius) {
        ArrayList<Boid> result = new ArrayList<>();
        int minCellX = cellX(position.x - radius);
        int maxCellX = cellX(position.x + radius);
        int minCellY = cellY(position.y - radius);
        int maxCellY = cellY(position.y + radius);

        for (int x = minCellX; x <= maxCellX; x++) {
            for (int y = minCellY; y <= maxCellY; y++) {
                long key = cellKey(x, y);
                ArrayList<Boid> cellBoids = cells.get(key);
                if (cellBoids == null) {
                    continue;
                }
                for (Boid boid : cellBoids) {
                    if (Vector.distance(position, boid.getPosition()) < radius) {
                        result.add(boid);
                    }
                }
            }
        }
        return result;
    }

    private int cellX(double x) {
        return (int) Math.floor(x / cellSize);
    }

    private int cellY(double y) {
        return (int) Math.floor(y / cellSize);
    }

    private long cellKey(int x, int y) {
        return (((long) x) << 32) | (y & 0xffffffffL);
    }
}
