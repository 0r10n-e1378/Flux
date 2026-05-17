/*
 * HUD.java
 *
 * Draws the heads-up display for the game.
 * Shows minion count, formation mode, health, XP, and pause overlay.
 */
package ui;

import core.Formation;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

public class HUD {

    public static void drawGameHUD(Graphics g, int minionCount, int maxMinions, int xp, int upgradeCost,
                                   Formation currentFormation, int commanderHealth, int width, int height,
                                   boolean paused) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Minions: " + minionCount + "/" + maxMinions, 20, 30);

        Color formationColor = Color.CYAN;
        String formationName = "NORMAL";
        if (currentFormation == Formation.HEX_SHIELD) {
            formationName = "HEX SHIELD";
            formationColor = Color.YELLOW;
        } else if (currentFormation == Formation.ARROWHEAD) {
            formationName = "ARROWHEAD";
            formationColor = Color.RED;
        } else if (currentFormation == Formation.PHALANX) {
            formationName = "PHALANX";
            formationColor = Color.GREEN;
        }

        g.setColor(formationColor);
        g.drawString("Formation: " + formationName + " (Press SPACE to cycle)", 20, 55);

        int barX = 20;
        int barY = height - 40;
        int barWidth = 200;
        int barHeight = 18;
        int fillWidth = Math.max(0, commanderHealth * barWidth / 100);

        g.setColor(Color.DARK_GRAY);
        g.fillRect(barX, barY, barWidth, barHeight);
        g.setColor(Color.RED);
        g.fillRect(barX, barY, fillWidth, barHeight);
        g.setColor(Color.WHITE);
        g.drawRect(barX, barY, barWidth, barHeight);
        g.drawString("HP: " + commanderHealth, barX + 8, barY + barHeight - 4);

        int xpBarX = width - 220;
        int xpBarY = height - 40;
        int xpBarWidth = 200;
        int xpBarHeight = 18;
        int xpFillWidth = Math.min(xpBarWidth, xp * xpBarWidth / Math.max(1, upgradeCost));

        g.setColor(Color.DARK_GRAY);
        g.fillRect(xpBarX, xpBarY, xpBarWidth, xpBarHeight);
        g.setColor(Color.MAGENTA);
        g.fillRect(xpBarX, xpBarY, xpFillWidth, xpBarHeight);
        g.setColor(Color.WHITE);
        g.drawRect(xpBarX, xpBarY, xpBarWidth, xpBarHeight);
        g.drawString("XP: " + xp + "/" + upgradeCost, xpBarX + 8, xpBarY + xpBarHeight - 4);

        if (paused) {
            drawPauseOverlay(g, width, height);
        }
    }

    public static void drawPauseOverlay(Graphics g, int width, int height) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, width, height);

        g.setFont(new Font("Arial", Font.BOLD, 72));
        g.setColor(Color.WHITE);
        String pausedText = "PAUSED";
        int textWidth = g.getFontMetrics().stringWidth(pausedText);
        g.drawString(pausedText, (width - textWidth) / 2, 120);

        int buttonWidth = 280;
        int buttonHeight = 90;
        int spacing = 30;
        int totalWidth = buttonWidth * 2 + spacing;
        int startX = (width - totalWidth) / 2;
        int buttonY = height / 2;

        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(startX, buttonY, buttonWidth, buttonHeight);
        g.fillRect(startX + buttonWidth + spacing, buttonY, buttonWidth, buttonHeight);

        g.setColor(Color.WHITE);
        g.drawRect(startX, buttonY, buttonWidth, buttonHeight);
        g.drawRect(startX + buttonWidth + spacing, buttonY, buttonWidth, buttonHeight);

        g.setFont(new Font("Arial", Font.BOLD, 28));
        String continueText = "CONTINUE";
        String exitText = "EXIT";
        int continueWidth = g.getFontMetrics().stringWidth(continueText);
        int exitWidth = g.getFontMetrics().stringWidth(exitText);
        g.drawString(continueText, startX + (buttonWidth - continueWidth) / 2, buttonY + 56);
        g.drawString(exitText, startX + buttonWidth + spacing + (buttonWidth - exitWidth) / 2, buttonY + 56);
    }
}
