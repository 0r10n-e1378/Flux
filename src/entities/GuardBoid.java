package entities;

import core.Camera;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import math.Vector;
import world.MapGenerator;

public class GuardBoid extends EnemyBoid {

    public GuardBoid(String id, int xPos, int yPos, int radius, Vector velocity, Vector acceleration) {
        super(id, xPos, yPos, radius, velocity, acceleration);
        maxSpeed = 5.0;
        maxForce = 0.18;
    }

    @Override
    public void draw(Graphics g, Camera camera) {
        int screenX = camera.getScreenX(position.x);
        int screenY = camera.getScreenY(position.y);
        double angle = velocity.magnitude() > 0 ? Math.atan2(velocity.y, velocity.x) : 0;

        Polygon guard = createGuardShape(screenX, screenY, radius, angle);
        g.setColor(new Color(170, 120, 240));
        g.fillPolygon(guard);
        g.setColor(Color.WHITE);
        g.drawPolygon(guard);
    }

    private Polygon createGuardShape(int x, int y, int radius, double angle) {
        int tipX = x + (int) (Math.cos(angle) * radius * 1.8);
        int tipY = y + (int) (Math.sin(angle) * radius * 1.8);

        int leftX = x + (int) (Math.cos(angle + Math.PI * 0.8) * radius);
        int leftY = y + (int) (Math.sin(angle + Math.PI * 0.8) * radius);
        int rightX = x + (int) (Math.cos(angle - Math.PI * 0.8) * radius);
        int rightY = y + (int) (Math.sin(angle - Math.PI * 0.8) * radius);

        int frontLeftX = x + (int) (Math.cos(angle + Math.PI * 0.55) * radius * 0.75);
        int frontLeftY = y + (int) (Math.sin(angle + Math.PI * 0.55) * radius * 0.75);
        int frontRightX = x + (int) (Math.cos(angle - Math.PI * 0.55) * radius * 0.75);
        int frontRightY = y + (int) (Math.sin(angle - Math.PI * 0.55) * radius * 0.75);

        int backLeftX = x - (int) (Math.cos(angle) * radius * 0.7) + (int) (Math.cos(angle + Math.PI / 2) * radius * 0.4);
        int backLeftY = y - (int) (Math.sin(angle) * radius * 0.7) + (int) (Math.sin(angle + Math.PI / 2) * radius * 0.4);
        int backRightX = x - (int) (Math.cos(angle) * radius * 0.7) - (int) (Math.cos(angle + Math.PI / 2) * radius * 0.4);
        int backRightY = y - (int) (Math.sin(angle) * radius * 0.7) - (int) (Math.sin(angle + Math.PI / 2) * radius * 0.4);

        return new Polygon(
            new int[] { leftX, frontLeftX, tipX, frontRightX, rightX, backRightX, x, backLeftX },
            new int[] { leftY, frontLeftY, tipY, frontRightY, rightY, backRightY, y, backLeftY },
            8
        );
    }
}