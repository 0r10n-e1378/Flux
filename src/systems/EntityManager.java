package systems;

import core.Camera;
import entities.Entity;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

public class EntityManager {
    private final List<Entity> entities = new ArrayList<>();

    public void add(Entity entity) {
        if (entity != null) {
            entities.add(entity);
        }
    }

    public void updateAll() {
        for (Entity entity : entities) {
            entity.update();
        }
    }

    public void drawAll(Graphics g, Camera camera) {
        for (Entity entity : entities) {
            entity.draw(g, camera);
        }
    }
}
