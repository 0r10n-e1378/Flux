package core;

import entities.Boid;
import entities.BossBoid;
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
        PLAYING,
        UPGRADE
    }

    private enum UpgradeType {
        NORMAL_BOIDS(0, "More Normal Boids"),
        SPEED(1, "Faster Speed"),
        MINION_SPAWN(2, "Minion Spawn Rate"),
        MAX_CAPACITY(3, "Max Capacity +10");

        final int id;
        final String name;

        UpgradeType(int id, String name) {
            this.id = id;
            this.name = name;
        }
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
    private int maxMinions = 75;
    private int xp = 0;
    private int upgradeCost = 10;
    private double normalBoidSpawnRate = 1.0; // multiplier for normal boid swarms
    private double speedMultiplier = 1.0; // for commander and minions
    private int minionSpawnRate = 0; // passive minion spawns per second
    private long lastMinionSpawnTime = 0;
    private List<Integer> availableUpgrades = new ArrayList<>();
    private List<Integer> lastAvailableUpgrades = new ArrayList<>();

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
        xp = 0;
        upgradeCost = 10;
        normalBoidSpawnRate = 1.0;
        speedMultiplier = 1.0;
        minionSpawnRate = 0;
        lastMinionSpawnTime = System.currentTimeMillis();
        generateRandomUpgrades();

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
            commander.setSpeedMultiplier(speedMultiplier);
            for (Minion minion : minions) {
                minion.setSpeedMultiplier(speedMultiplier);
            }
            mapGenerator.setNormalBoidSpawnRate(normalBoidSpawnRate);
            commander.update(input, mapGenerator);

            ArrayList<NormalBoid> loadedNormals = mapGenerator.getLoadedNormalBoids();
            ArrayList<EnemyBoid> loadedEnemies = mapGenerator.getLoadedEnemyBoids();

            // Create list of all boids for avoidance
            ArrayList<Boid> allBoids = new ArrayList<>();
            allBoids.addAll(loadedNormals);
            allBoids.addAll(loadedEnemies);
            allBoids.addAll(minions);

            ArrayList<NormalBoid> converted = new ArrayList<>();
            for (NormalBoid normal : loadedNormals) {
                // Check for conversion based on nearby minions
                int nearbyMinions = 0;
                for (Minion minion : minions) {
                    if (Vector.distance(normal.getPosition(), minion.getPosition()) < 100) {
                        nearbyMinions++;
                    }
                }
                // Also check commander proximity
                double commanderDist = Vector.distance(normal.getPosition(), commander.getPosition());
                if (nearbyMinions >= 5 || commanderDist < 90) {
                    converted.add(normal);
                }
            }
            for (NormalBoid normal : converted) {
                if (minions.size() < maxMinions) {
                    mapGenerator.removeNormalBoid(normal);
                    minions.add(spawner.spawnMinion(normal.getX(), normal.getY(), normal.getVelocity()));
                }
            }

            loadedNormals = mapGenerator.getLoadedNormalBoids();
            for (NormalBoid normal : loadedNormals) {
                normal.update(allBoids, mapGenerator);
            }

            ArrayList<BossBoid> loadedBosses = mapGenerator.getLoadedBossBoids();
            for (BossBoid boss : loadedBosses) {
                boss.update(new ArrayList<>(loadedEnemies), commander, mapGenerator);
            }

            for (EnemyBoid enemy : loadedEnemies) {
                // If there are bosses, flock to them; otherwise chase commander
                if (!loadedBosses.isEmpty()) {
                    BossBoid nearestBoss = loadedBosses.get(0);
                    double nearestDist = Vector.distance(enemy.getPosition(), nearestBoss.getPosition());
                    for (BossBoid boss : loadedBosses) {
                        double dist = Vector.distance(enemy.getPosition(), boss.getPosition());
                        if (dist < nearestDist) {
                            nearestBoss = boss;
                            nearestDist = dist;
                        }
                    }
                    enemy.updateWithTarget(new ArrayList<>(loadedEnemies), nearestBoss.getPosition(), mapGenerator);
                } else {
                    enemy.update(new ArrayList<>(loadedEnemies), commander, mapGenerator);
                }
            }

            ArrayList<EnemyBoid> enemiesToRemove = new ArrayList<>();
            ArrayList<Minion> minionsToRemove = new ArrayList<>();
            ArrayList<BossBoid> bossesToRemove = new ArrayList<>();
            
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
                        xp += 1; // Gain XP for killing enemy
                        break;
                    }
                }
            }
            
            // Handle boss collisions
            for (BossBoid boss : loadedBosses) {
                double distCommander = Vector.distance(boss.getPosition(), commander.getPosition());
                if (distCommander < boss.getRadius() + commander.getRadius()) {
                    boss.takeDamage(1);
                    if (boss.isDead()) {
                        bossesToRemove.add(boss);
                        xp += 50; // Boss gives 50 XP
                    } else {
                        commander.takeDamage(1);
                    }
                    continue;
                }
                for (Minion minion : minions) {
                    double dist = Vector.distance(boss.getPosition(), minion.getPosition());
                    if (dist < boss.getRadius() + minion.getRadius()) {
                        boss.takeDamage(1);
                        if (boss.isDead()) {
                            bossesToRemove.add(boss);
                            xp += 50; // Boss gives 50 XP
                            break;
                        } else {
                            minionsToRemove.add(minion);
                            break;
                        }
                    }
                }
            }
            
            for (EnemyBoid enemy : enemiesToRemove) {
                mapGenerator.removeEnemyBoid(enemy);
            }
            for (BossBoid boss : bossesToRemove) {
                mapGenerator.removeBossBoid(boss);
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

            // Check for upgrade
            if (xp >= upgradeCost) {
                generateRandomUpgrades();
                gameState = GameState.UPGRADE;
                return;
            }

            // Passive minion spawn
            long currentTime = System.currentTimeMillis();
            if (minionSpawnRate > 0 && minions.size() < maxMinions && currentTime - lastMinionSpawnTime >= 1000 / minionSpawnRate) {
                double angle = Math.random() * Math.PI * 2;
                double distance = 100;
                double mx = commander.getX() + Math.cos(angle) * distance;
                double my = commander.getY() + Math.sin(angle) * distance;
                minions.add(spawner.spawnMinion(mx, my));
                lastMinionSpawnTime = currentTime;
            }

            for (Minion minion : minions) {
                minion.update(commander, new ArrayList<>(minions), mapGenerator);
            }
            mapGenerator.update(commander.getX(), commander.getY());
            entityManager.updateAll();
            camera.update(commander.getX(), commander.getY());
        }
    }

    private void generateRandomUpgrades() {
        availableUpgrades.clear();
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            all.add(i);
        }
        
        // Remove previously shown upgrades only if we still have more than 3 options left
        if (lastAvailableUpgrades.size() > 0 && all.size() - lastAvailableUpgrades.size() >= 3) {
            for (Integer prev : lastAvailableUpgrades) {
                all.remove(prev);
            }
        }
        
        java.util.Collections.shuffle(all);
        for (int i = 0; i < 3 && i < all.size(); i++) {
            availableUpgrades.add(all.get(i));
        }
        lastAvailableUpgrades = new ArrayList<>(availableUpgrades);
    }

    private void applyUpgrade(int upgradeIndex) {
        int upgradeId = availableUpgrades.get(upgradeIndex);
        switch (upgradeId) {
            case 0: // NORMAL_BOIDS
                normalBoidSpawnRate += 0.5;
                break;
            case 1: // SPEED
                speedMultiplier += 0.2;
                break;
            case 2: // MINION_SPAWN
                minionSpawnRate++;
                break;
            case 3: // MAX_CAPACITY
                maxMinions += 10;
                break;
        }
        xp -= upgradeCost;
        upgradeCost = (int) (upgradeCost * 1.5);
        gameState = GameState.PLAYING;
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (gameState == GameState.MENU) {
            if (menuScreen != null) {
                menuScreen.draw(g);
            }
        } else if (gameState == GameState.PLAYING) {
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
            ArrayList<BossBoid> loadedBosses = mapGenerator.getLoadedBossBoids();
            for (BossBoid boss : loadedBosses) {
                boss.draw(g, camera);
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
            // Minion count in top left
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.drawString("Minions: " + minions.size() + "/" + maxMinions, 20, 30);
            // XP bar bottom right
            int xpBarX = getWidth() - 220;
            int xpBarY = getHeight() - 40;
            int xpBarWidth = 200;
            int xpBarHeight = 18;
            int xpFillWidth = Math.min(xpBarWidth, xp * xpBarWidth / upgradeCost);

            g.setColor(Color.DARK_GRAY);
            g.fillRect(xpBarX, xpBarY, xpBarWidth, xpBarHeight);
            g.setColor(Color.MAGENTA);
            g.fillRect(xpBarX, xpBarY, xpFillWidth, xpBarHeight);
            g.setColor(Color.WHITE);
            g.drawRect(xpBarX, xpBarY, xpBarWidth, xpBarHeight);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            g.drawString("XP: " + xp + "/" + upgradeCost, xpBarX + 8, xpBarY + xpBarHeight - 4);
        } else if (gameState == GameState.UPGRADE) {
            // Draw upgrade screen
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            String upgradeText = "Choose an Upgrade!";
            int textWidth = g.getFontMetrics().stringWidth(upgradeText);
            g.drawString(upgradeText, (getWidth() - textWidth) / 2, 100);

            // Draw 3 random upgrade buttons
            int buttonWidth = 280;
            int buttonHeight = 100;
            int startX = (getWidth() - (buttonWidth * 3 + 40)) / 2;
            int startY = 250;
            int spacing = 20;

            Color[] colors = { Color.BLUE, Color.GREEN, Color.ORANGE, Color.CYAN };
            String[] upgrades = { "More Normal Boids", "Faster Speed", "Minion Spawn Rate", "Max Capacity +10" };

            for (int i = 0; i < 3 && i < availableUpgrades.size(); i++) {
                int upgradeId = availableUpgrades.get(i);
                int buttonX = startX + i * (buttonWidth + spacing);
                int buttonY = startY;

                g.setColor(colors[upgradeId]);
                g.fillRect(buttonX, buttonY, buttonWidth, buttonHeight);
                g.setColor(Color.WHITE);
                ((java.awt.Graphics2D) g).setStroke(new java.awt.BasicStroke(3));
                ((java.awt.Graphics2D) g).drawRect(buttonX, buttonY, buttonWidth, buttonHeight);
                g.setFont(new Font("Arial", Font.BOLD, 18));
                String text = upgrades[upgradeId];
                int textW = g.getFontMetrics().stringWidth(text);
                g.drawString(text, buttonX + (buttonWidth - textW) / 2, buttonY + 55);
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
    public void mousePressed(MouseEvent e) {
        if (gameState == GameState.MENU && menuScreen != null) {
            if (menuScreen.isClicked(e.getX(), e.getY())) {
                initGame();
            }
        } else if (gameState == GameState.UPGRADE) {
            int buttonWidth = 280;
            int buttonHeight = 100;
            int startX = (getWidth() - (buttonWidth * 3 + 40)) / 2;
            int startY = 250;
            int spacing = 20;

            for (int i = 0; i < 3 && i < availableUpgrades.size(); i++) {
                int buttonX = startX + i * (buttonWidth + spacing);
                int buttonY = startY;

                if (e.getX() >= buttonX && e.getX() <= buttonX + buttonWidth && e.getY() >= buttonY && e.getY() <= buttonY + buttonHeight) {
                    applyUpgrade(i);
                    break;
                }
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // No-op - using mousePressed for better responsiveness
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
