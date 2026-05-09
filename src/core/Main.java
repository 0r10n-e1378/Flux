package core;

import entities.Commander;
import entities.EnemyBoid;
import entities.Minion;
import entities.NormalBoid;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import math.Vector;
import systems.EntityManager;
import ui.Menu;
import world.MapGenerator;
import world.Spawner;

public class Main extends JPanel implements Runnable, KeyListener, MouseListener {
    private enum GameState {
        MENU,
        PLAYING
    }

    private JFrame window;
    private Thread gameThread;
    private Menu menuScreen;
    private boolean isRunning = true;

    private GameState gameState = GameState.MENU;
    private Input input;
    private Camera camera;
    private EntityManager entityManager;
    private MapGenerator mapGenerator;
    private Spawner spawner;
    private Commander commander;
    private List<Minion> minions;

    public Main() {
        // Setup the Window
        window = new JFrame("Flux");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setExtendedState(JFrame.MAXIMIZED_BOTH); // Fullscreen fallback
        window.setUndecorated(true);
        window.setSize(1280, 720);
        window.setLocationRelativeTo(null);

        // Setup this Panel (the drawing surface)
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true); // Prevents flickering
        this.setFocusable(true);
        this.setFocusTraversalKeysEnabled(false);
        this.addKeyListener(this);
        this.addMouseListener(this);

        window.add(this);
        window.setVisible(true);
        window.addKeyListener(this);

        // Initialize Menu
        menuScreen = new Menu(window.getWidth(), window.getHeight());
        input = new Input();

        // Start the game thread
        gameThread = new Thread(this);
        gameThread.start();
    }

    private void initGame() {
        camera = new Camera(getWidth(), getHeight());
        entityManager = new EntityManager();
        mapGenerator = new MapGenerator();
        spawner = new Spawner();
        commander = spawner.spawnCommander(0, 0);
        commander.resetHealth();
        minions = new ArrayList<>();

        for (int i = 0; i < 30; i++) {
            double angle = Math.toRadians(i * 12);
            double distance = 80 + (i % 6) * 6;
            double mx = commander.getX() + Math.cos(angle) * distance;
            double my = commander.getY() + Math.sin(angle) * distance;
            minions.add(spawner.spawnMinion(mx, my));
        }

        mapGenerator.update(commander.getX(), commander.getY());
        camera.update(commander.getX(), commander.getY());
        gameState = GameState.PLAYING;
        requestFocusInWindow();
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
        if (gameState == GameState.PLAYING) {
            commander.update(input, mapGenerator);

            ArrayList<NormalBoid> loadedNormals = mapGenerator.getLoadedNormalBoids();
            ArrayList<NormalBoid> converted = new ArrayList<>();
            for (NormalBoid normal : loadedNormals) {
                if (Vector.distance(normal.getPosition(), commander.getPosition()) < 90) {
                    converted.add(normal);
                }
            }
            for (NormalBoid normal : converted) {
                mapGenerator.removeNormalBoid(normal);
                minions.add(spawner.spawnMinion(normal.getX(), normal.getY(), normal.getVelocity()));
            }

            loadedNormals = mapGenerator.getLoadedNormalBoids();
            for (NormalBoid normal : loadedNormals) {
                normal.update(new ArrayList<>(loadedNormals), mapGenerator);
            }

            ArrayList<EnemyBoid> loadedEnemies = mapGenerator.getLoadedEnemyBoids();
            for (EnemyBoid enemy : loadedEnemies) {
                enemy.update(new ArrayList<>(loadedEnemies), commander, mapGenerator);
            }

            ArrayList<EnemyBoid> enemiesToRemove = new ArrayList<>();
            ArrayList<Minion> minionsToRemove = new ArrayList<>();
            for (EnemyBoid enemy : loadedEnemies) {
                double distCommander = Vector.distance(enemy.getPosition(), commander.getPosition());
                if (distCommander < enemy.getRadius() + commander.getRadius()) {
                    enemiesToRemove.add(enemy);
                    commander.takeDamage(1);
                    continue;
                }
                for (Minion minion : minions) {
                    double dist = Vector.distance(enemy.getPosition(), minion.getPosition());
                    if (dist < enemy.getRadius() + minion.getRadius()) {
                        enemiesToRemove.add(enemy);
                        minionsToRemove.add(minion);
                        break;
                    }
                }
            }
            for (EnemyBoid enemy : enemiesToRemove) {
                mapGenerator.removeEnemyBoid(enemy);
            }
            minions.removeAll(minionsToRemove);

            if (commander.getHealth() <= 0) {
                gameState = GameState.MENU;
                mapGenerator = null;
                commander = null;
                minions = null;
                entityManager = null;
                camera = null;
                spawner = null;
                return;
            }

            for (Minion minion : minions) {
                minion.update(commander, new ArrayList<>(minions), mapGenerator);
            }
            mapGenerator.update(commander.getX(), commander.getY());
            entityManager.updateAll();
            camera.update(commander.getX(), commander.getY());
        }
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (gameState == GameState.MENU) {
            if (menuScreen != null) {
                menuScreen.draw(g);
            }
        } else {
            if (mapGenerator != null) {
                mapGenerator.draw(g, camera);
            }
            ArrayList<NormalBoid> loadedNormals = mapGenerator.getLoadedNormalBoids();
            for (NormalBoid normal : loadedNormals) {
                normal.draw(g, camera);
            }
            ArrayList<EnemyBoid> loadedEnemies = mapGenerator.getLoadedEnemyBoids();
            for (EnemyBoid enemy : loadedEnemies) {
                enemy.draw(g, camera);
            }
            if (minions != null) {
                for (Minion minion : minions) {
                    minion.draw(g, camera);
                }
            }
            if (commander != null) {
                commander.draw(g, camera);
            }
            if (entityManager != null) {
                entityManager.drawAll(g, camera);
            }
            if (commander != null) {
                int barX = 20;
                int barY = getHeight() - 40;
                int barWidth = 200;
                int barHeight = 18;
                int fillWidth = Math.max(0, commander.getHealth() * barWidth / 100);

                g.setColor(Color.DARK_GRAY);
                g.fillRect(barX, barY, barWidth, barHeight);
                g.setColor(Color.RED);
                g.fillRect(barX, barY, fillWidth, barHeight);
                g.setColor(Color.WHITE);
                g.drawRect(barX, barY, barWidth, barHeight);
                g.setFont(new Font("Arial", Font.BOLD, 14));
                g.drawString("HP: " + commander.getHealth(), barX + 8, barY + barHeight - 4);
            }
        }
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(Main::new);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameState == GameState.MENU && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            System.exit(0);
        }
        input.setKeyState(e.getKeyCode(), true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        input.setKeyState(e.getKeyCode(), false);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // No-op
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (gameState == GameState.MENU && menuScreen != null) {
            if (menuScreen.isClicked(e.getX(), e.getY())) {
                initGame();
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // No-op
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // No-op
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // No-op
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // No-op
    }
}
