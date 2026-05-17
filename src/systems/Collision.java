/*
 * Collision.java
 *
 * Utility class for basic collision checks.
 * Currently supports circle-vs-rectangle intersection tests.
 */
package systems;

import java.awt.Rectangle;

public class Collision {

    public static boolean circleIntersectsRect(double cx, double cy, int radius, Rectangle rect) {
        double closestX = Math.max(rect.x, Math.min(cx, rect.x + rect.width));
        double closestY = Math.max(rect.y, Math.min(cy, rect.y + rect.height));
        double dx = cx - closestX;
        double dy = cy - closestY;
        return dx * dx + dy * dy < radius * radius;
    }
}
