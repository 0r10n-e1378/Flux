package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;

public class Menu {

	private String title = "Flux";
	private Rectangle playButton;
	
	public Menu (int width, int height) {
		int btnWidth = 200;
	    int btnHeight = 60;
	    this.playButton = new Rectangle((width / 2) - (btnWidth / 2), 400, btnWidth, btnHeight);
	}
	
	public void draw(Graphics g) {
	    int screenWidth = g.getClipBounds().width;
	    int screenHeight = g.getClipBounds().height;

	    // Background
	    g.setColor(Color.BLACK);
	    g.fillRect(0, 0, screenWidth, screenHeight);

	    // Center the Title
	    g.setColor(Color.CYAN);
	    Font titleFont = new Font("Monospaced", Font.BOLD, 80);
	    g.setFont(titleFont);
	    int titleWidth = g.getFontMetrics().stringWidth(title);
	    g.drawString(title, (screenWidth / 2) - (titleWidth / 2), 200);

	    // Button
	    g.setColor(Color.WHITE);
	    g.drawRect(playButton.x, playButton.y, playButton.width, playButton.height);
	    g.setFont(new Font("Arial", Font.PLAIN, 20));
	    String buttonText = "START HIVE";
	    
	    // Font
	    var metrics = g.getFontMetrics();
	    int textWidth = metrics.stringWidth(buttonText);
	    int textHeight = metrics.getHeight();
	    int textX = playButton.x + (playButton.width - textWidth) / 2;
	    int textY = playButton.y + ((playButton.height - textHeight) / 2) + metrics.getAscent();

	    g.drawString(buttonText, textX, textY);
	}
	
	public boolean isClicked(int mouseX, int mouseY) {
        return playButton.contains(mouseX, mouseY);
    }
	
}
