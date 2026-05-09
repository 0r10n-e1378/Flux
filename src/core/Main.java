package core;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import ui.Menu;

public class Main extends JPanel implements Runnable {
    
    private JFrame window;
    private Thread gameThread;
    private Menu menuScreen;
    private boolean isRunning = true;

    public Main() {
        // Setup the Window
        window = new JFrame("Flux");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setExtendedState(JFrame.MAXIMIZED_BOTH); // Fullscreen
        window.setUndecorated(true);
        
        // Setup this Panel (the drawing surface)
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true); // Prevents flickering
        
        window.add(this);
        window.setVisible(true);

        // Initialize Menu
        menuScreen = new Menu(window.getWidth(), window.getHeight());

        // Start the Game Thread
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void run() {
        // Basic Game Loop
        while (isRunning) {
            update();
            repaint(); // Calls paintComponent

            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void update() {
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Draw Menu
        if (menuScreen != null) {
            menuScreen.draw(g);
        }
        
        g.dispose();
    }

    public static void main(String[] args) {
        new Main();
    }
}