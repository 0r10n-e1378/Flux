package entities;

import core.Camera;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

public class Wall extends Entity {
    private int width;
    private int height;
    private Color fillColor = new Color(60, 60, 68);
    private Color borderColor = new Color(140, 140, 160);

    public Wall(double centerX, double centerY, int width, int height) {
        super(centerX, centerY, 0);
        this.width = width;
        this.height = height;
    }

    public Rectangle getBounds() {
        return new Rectangle(
            (int) Math.round(position.x - width / 2.0),
            (int) Math.round(position.y - height / 2.0),
            width,
            height
        );
    }

    @Override
    public void draw(Graphics g, Camera camera) {
        Rectangle bounds = getBounds();
        int screenX = camera.getScreenX(bounds.x);
        int screenY = camera.getScreenY(bounds.y);

        g.setColor(fillColor);
        g.fillRect(screenX, screenY, bounds.width, bounds.height);
        g.setColor(borderColor);
        g.drawRect(screenX, screenY, bounds.width, bounds.height);
    }
}
