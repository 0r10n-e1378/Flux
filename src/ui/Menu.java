package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;

public class Menu {

	private String title = "Flux";
	private int buttonWidth = 200;
	private int buttonHeight = 60;
	private int screenWidth;
	private int screenHeight;
	
	public Menu(int width, int height) {
		this.screenWidth = width;
		this.screenHeight = height;
	}
	
	public void draw(Graphics g) {
	    int currentWidth = g.getClipBounds().width;
	    int currentHeight = g.getClipBounds().height;
	    this.screenWidth = currentWidth;
	    this.screenHeight = currentHeight;
	    int buttonX = (currentWidth / 2) - (buttonWidth / 2);
	    int buttonY = currentHeight / 2;
	
	    // Background
	    g.setColor(Color.BLACK);
	    g.fillRect(0, 0, currentWidth, currentHeight);

	    // Center the Title
	    g.setColor(Color.CYAN);
	    Font titleFont = new Font("Monospaced", Font.BOLD, 80);
	    g.setFont(titleFont);
	    int titleWidth = g.getFontMetrics().stringWidth(title);
	    g.drawString(title, (currentWidth / 2) - (titleWidth / 2), 200);

	    // Button
	    g.setColor(Color.WHITE);
	    g.drawRect(buttonX, buttonY, buttonWidth, buttonHeight);
	    g.setFont(new Font("Arial", Font.PLAIN, 20));
	    String buttonText = "START HIVE";
    	
	    var metrics = g.getFontMetrics();
	    int textWidth = metrics.stringWidth(buttonText);
	    int textHeight = metrics.getHeight();
	    int textX = buttonX + (buttonWidth - textWidth) / 2;
	    int textY = buttonY + ((buttonHeight - textHeight) / 2) + metrics.getAscent();

	    g.drawString(buttonText, textX, textY);
	}

	public boolean isClicked(int mouseX, int mouseY) {
	    int buttonX = (screenWidth / 2) - (buttonWidth / 2);
	    int buttonY = screenHeight / 2;
	    return new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight).contains(mouseX, mouseY);
	}
}
