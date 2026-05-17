/*
 * Input.java
 *
 * Tracks keyboard input states for movement keys.
 * Provides a normalized movement vector to drive the player commander.
 */
package core;

import math.Vector;
import java.awt.event.KeyEvent;

public class Input {
    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;

    public void setKeyState(int keyCode, boolean pressed) {
        switch (keyCode) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                up = pressed;
                break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                down = pressed;
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                left = pressed;
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                right = pressed;
                break;
            default:
                break;
        }
    }

    // Convert the current keyboard state into a direction vector.
    // The vector is normalized so diagonal movement is not faster than straight movement.
    public Vector getDirection() {
        Vector direction = new Vector(0, 0);

        if (up) {
            direction.y -= 1;
        }
        if (down) {
            direction.y += 1;
        }
        if (left) {
            direction.x -= 1;
        }
        if (right) {
            direction.x += 1;
        }

        if (direction.magnitude() > 0) {
            direction.normalize();
        }

        return direction;
    }
}
