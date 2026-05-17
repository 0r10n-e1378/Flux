/*
 * Camera.java
 *
 * Translates world coordinates into screen coordinates.
 * The camera centers on a target position so the world can scroll.
 */
package core;

public class Camera {
    private int screenWidth;
    private int screenHeight;
    private double offsetX;
    private double offsetY;

    public Camera(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public void update(double targetX, double targetY) {
        this.offsetX = targetX - screenWidth / 2.0;
        this.offsetY = targetY - screenHeight / 2.0;
    }

    public int getScreenX(double worldX) {
        return (int) Math.round(worldX - offsetX);
    }

    public int getScreenY(double worldY) {
        return (int) Math.round(worldY - offsetY);
    }
}
