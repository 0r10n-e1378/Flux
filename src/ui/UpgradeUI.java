package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;

public class UpgradeUI {
    private Rectangle buttonSpawns, buttonSpeed, buttonMinions;
    private int screenWidth, screenHeight;

    public UpgradeUI(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        int buttonWidth = 250;
        int buttonHeight = 80;
        int spacing = 30;
        int totalWidth = (buttonWidth * 3) + (spacing * 2);
        int startX = (width - totalWidth) / 2;
        int startY = (height - buttonHeight) / 2;

        buttonSpawns = new Rectangle(startX, startY, buttonWidth, buttonHeight);
        buttonSpeed = new Rectangle(startX + buttonWidth + spacing, startY, buttonWidth, buttonHeight);
        buttonMinions = new Rectangle(startX + (buttonWidth + spacing) * 2, startY, buttonWidth, buttonHeight);
    }

    public void draw(Graphics g) {
        int textX, textY;
        Font font = new Font("Arial", Font.BOLD, 16);
        g.setFont(font);

        // Semi-transparent overlay
        g.setColor(new Color(0, 0, 0, 128));
        g.fillRect(0, 0, screenWidth, screenHeight);

        // Title
        g.setColor(Color.CYAN);
        Font titleFont = new Font("Arial", Font.BOLD, 28);
        g.setFont(titleFont);
        String title = "CHOOSE AN UPGRADE";
        int titleWidth = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (screenWidth - titleWidth) / 2, 100);

        // Button 1: Spawn Boids
        g.setColor(Color.WHITE);
        g.drawRect(buttonSpawns.x, buttonSpawns.y, buttonSpawns.width, buttonSpawns.height);
        g.setColor(new Color(100, 200, 100, 200));
        g.fillRect(buttonSpawns.x, buttonSpawns.y, buttonSpawns.width, buttonSpawns.height);
        g.setColor(Color.WHITE);
        g.setFont(font);
        textX = buttonSpawns.x + 15;
        textY = buttonSpawns.y + 30;
        g.drawString("More Boids", textX, textY);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Spawn rate + 25%", textX, textY + 20);

        // Button 2: Speed
        g.setColor(Color.WHITE);
        g.drawRect(buttonSpeed.x, buttonSpeed.y, buttonSpeed.width, buttonSpeed.height);
        g.setColor(new Color(100, 100, 200, 200));
        g.fillRect(buttonSpeed.x, buttonSpeed.y, buttonSpeed.width, buttonSpeed.height);
        g.setColor(Color.WHITE);
        g.setFont(font);
        textX = buttonSpeed.x + 35;
        textY = buttonSpeed.y + 30;
        g.drawString("Speed Boost", textX, textY);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("+15% Movement", textX + 5, textY + 20);

        // Button 3: Passive Minion Spawn
        g.setColor(Color.WHITE);
        g.drawRect(buttonMinions.x, buttonMinions.y, buttonMinions.width, buttonMinions.height);
        g.setColor(new Color(200, 100, 200, 200));
        g.fillRect(buttonMinions.x, buttonMinions.y, buttonMinions.width, buttonMinions.height);
        g.setColor(Color.WHITE);
        g.setFont(font);
        textX = buttonMinions.x + 20;
        textY = buttonMinions.y + 30;
        g.drawString("Minion Spawn", textX, textY);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("1 per 15 frames", textX + 10, textY + 20);
    }

    public int getClickedUpgrade(int mouseX, int mouseY) {
        if (buttonSpawns.contains(mouseX, mouseY)) {
            return 0; // Spawn rate
        }
        if (buttonSpeed.contains(mouseX, mouseY)) {
            return 1; // Speed
        }
        if (buttonMinions.contains(mouseX, mouseY)) {
            return 2; // Minion spawn
        }
        return -1;
    }
}
