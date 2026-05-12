package core;

import entities.Boid;
import entities.BossBoid;
import entities.Commander;
import entities.EnemyBoid;
import entities.FlankerBoid;
import entities.Minion;
import entities.NormalBoid;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import math.Vector;
import systems.SwarmController;
import systems.FormationHelper;
import ui.HUD;
import ui.Menu;
import world.MapGenerator;
import world.Spawner;

public class Main extends JPanel implements Runnable, KeyListener, MouseListener {
    private enum GameState {
        MENU,
        PLAYING,
        PAUSED,
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
    
    private Formation currentFormation = Formation.NORMAL;
    private long lastFormationSwitchTime = 0;
    private static final long FORMATION_SWITCH_COOLDOWN = 200; // ms

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
        currentFormation = Formation.NORMAL;
        lastFormationSwitchTime = 0;
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
                ArrayList<Boid> neighbors = SwarmController.getNeighbors(normal.getPosition(), 120, loadedNormals, loadedEnemies, minions);
                normal.update(neighbors, mapGenerator);
            }

            ArrayList<BossBoid> loadedBosses = new ArrayList<>(mapGenerator.getLoadedBossBoids());
            for (BossBoid boss : loadedBosses) {
                boss.update(new ArrayList<>(loadedEnemies), commander, mapGenerator);
            }

            for (EnemyBoid enemy : new ArrayList<>(loadedEnemies)) {
                ArrayList<Boid> enemyNeighbors = SwarmController.getNeighbors(enemy.getPosition(), 100, new ArrayList<>(), loadedEnemies, new ArrayList<>());
                // If there are bosses, flock to them; otherwise chase commander
                // But flankers never flock to bosses
                if (!loadedBosses.isEmpty() && !(enemy instanceof FlankerBoid)) {
                    BossBoid nearestBoss = loadedBosses.get(0);
                    double nearestDist = Vector.distance(enemy.getPosition(), nearestBoss.getPosition());
                    for (BossBoid boss : loadedBosses) {
                        double dist = Vector.distance(enemy.getPosition(), boss.getPosition());
                        if (dist < nearestDist) {
                            nearestBoss = boss;
                            nearestDist = dist;
                        }
                    }
                    ArrayList<EnemyBoid> enemyOnlyNeighbors = new ArrayList<>();
                    for (Boid b : enemyNeighbors) {
                        if (b instanceof EnemyBoid) {
                            enemyOnlyNeighbors.add((EnemyBoid) b);
                        }
                    }
                    // Get enemies around this boss
                    ArrayList<EnemyBoid> bossEnemies = new ArrayList<>();
                    for (EnemyBoid e : loadedEnemies) {
                        if (Vector.distance(e.getPosition(), nearestBoss.getPosition()) < 200 && !(e instanceof FlankerBoid)) { // Arbitrary range
                            bossEnemies.add(e);
                        }
                    }
                    int enemyIndex = bossEnemies.indexOf(enemy);
                    if (enemyIndex >= 0) {
                        Vector formationTarget = FormationHelper.getBossFormationPosition(enemyIndex, bossEnemies.size(), nearestBoss.getPosition(), nearestBoss.getVelocity().magnitude() > 0 ? Math.atan2(nearestBoss.getVelocity().y, nearestBoss.getVelocity().x) : 0, nearestBoss.getFormation());
                        enemy.updateWithTarget(enemyOnlyNeighbors, formationTarget, mapGenerator);
                    } else {
                        enemy.updateWithTarget(enemyOnlyNeighbors, nearestBoss.getPosition(), mapGenerator);
                    }
                } else {
                    ArrayList<EnemyBoid> enemyOnlyNeighbors = new ArrayList<>();
                    for (Boid b : enemyNeighbors) {
                        if (b instanceof EnemyBoid) {
                            enemyOnlyNeighbors.add((EnemyBoid) b);
                        }
                    }
                    enemy.update(enemyOnlyNeighbors, commander, mapGenerator);
                }
            }

            ArrayList<EnemyBoid> enemiesToRemove = new ArrayList<>();
            ArrayList<Minion> minionsToRemove = new ArrayList<>();
            ArrayList<BossBoid> bossesToRemove = new ArrayList<>();
            
            // Handle enemy collisions
            for (EnemyBoid enemy : new ArrayList<>(loadedEnemies)) {
                double distCommander = Vector.distance(enemy.getPosition(), commander.getPosition());
                if (distCommander < enemy.getRadius() + commander.getRadius()) {
                    enemiesToRemove.add(enemy);
                    commander.takeDamage(1);
                    continue;
                }
                for (Minion minion : new ArrayList<>(minions)) {
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
            for (BossBoid boss : new ArrayList<>(loadedBosses)) {
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
                for (Minion minion : new ArrayList<>(minions)) {
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

            for (int i = 0; i < minions.size(); i++) {
                Minion minion = minions.get(i);
                Vector formationTarget = FormationHelper.getFormationPosition(i, minions.size(), commander.getPosition(), commander.getVelocity().magnitude() > 0 ? Math.atan2(commander.getVelocity().y, commander.getVelocity().x) : 0, currentFormation);
                ArrayList<Boid> minionNeighbors = SwarmController.getNeighbors(minion.getPosition(), 120, new ArrayList<>(), new ArrayList<>(), minions);
                minion.update(commander, minionNeighbors, mapGenerator, formationTarget, currentFormation == Formation.NORMAL ? 4.8 : 12.0);
            }
            mapGenerator.update(commander.getX(), commander.getY());
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
        } else if (gameState == GameState.PLAYING || gameState == GameState.PAUSED) {
            if (mapGenerator != null) {
                mapGenerator.draw(g, camera);
            }
            ArrayList<NormalBoid> loadedNormals = new ArrayList<>(mapGenerator.getLoadedNormalBoids());
            for (NormalBoid normal : loadedNormals) {
                normal.draw(g, camera);
            }
            ArrayList<EnemyBoid> loadedEnemies = new ArrayList<>(mapGenerator.getLoadedEnemyBoids());
            for (EnemyBoid enemy : loadedEnemies) {
                enemy.draw(g, camera);
            }
            ArrayList<BossBoid> loadedBosses = new ArrayList<>(mapGenerator.getLoadedBossBoids());
            for (BossBoid boss : loadedBosses) {
                boss.draw(g, camera);
            }
            if (minions != null) {
                ArrayList<Minion> minionsCopy = new ArrayList<>(minions);
                for (Minion minion : minionsCopy) {
                    minion.draw(g, camera);
                }
            }
            if (commander != null) {
                commander.draw(g, camera);
            }
            HUD.drawGameHUD(g, minions.size(), maxMinions, xp, upgradeCost, currentFormation, commander.getHealth(), getWidth(), getHeight(), gameState == GameState.PAUSED);
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
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            if (gameState == GameState.PLAYING) {
                gameState = GameState.PAUSED;
                return;
            } else if (gameState == GameState.PAUSED) {
                gameState = GameState.PLAYING;
                return;
            } else if (gameState == GameState.MENU) {
                System.exit(0);
                return;
            }
        }
        
        // Formation cycling with SPACE key during gameplay
        if (e.getKeyCode() == KeyEvent.VK_SPACE && gameState == GameState.PLAYING) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFormationSwitchTime >= FORMATION_SWITCH_COOLDOWN) {
                // Cycle to next formation
                currentFormation = Formation.values()[(currentFormation.ordinal() + 1) % Formation.values().length];
                lastFormationSwitchTime = currentTime;
                return;
            }
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
        } else if (gameState == GameState.PAUSED) {
            int buttonWidth = 280;
            int buttonHeight = 90;
            int spacing = 30;
            int totalWidth = buttonWidth * 2 + spacing;
            int startX = (getWidth() - totalWidth) / 2;
            int buttonY = getHeight() / 2;

            if (e.getX() >= startX && e.getX() <= startX + buttonWidth && e.getY() >= buttonY && e.getY() <= buttonY + buttonHeight) {
                gameState = GameState.PLAYING;
            } else if (e.getX() >= startX + buttonWidth + spacing && e.getX() <= startX + 2 * buttonWidth + spacing && e.getY() >= buttonY && e.getY() <= buttonY + buttonHeight) {
                gameState = GameState.MENU;
                mapGenerator = null;
                commander = null;
                minions = null;
                menuScreen = new Menu(getWidth(), getHeight());
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
