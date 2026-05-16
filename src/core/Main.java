package core;

import entities.Boid;
import entities.BossBoid;
import entities.Commander;
import entities.EnemyBoid;
import entities.GuardBoid;
import entities.Minion;
import entities.NormalBoid;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import math.Vector;
import systems.SwarmController;
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
                // If there are bosses, guards flock to them; otherwise chase commander
                // Scouts never flock to bosses
                if (!loadedBosses.isEmpty() && enemy instanceof GuardBoid) {
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
                    enemy.updateWithTarget(enemyOnlyNeighbors, nearestBoss.getPosition(), mapGenerator);
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
                        xp += enemy instanceof GuardBoid ? 2 : 1; // Gain XP for killing enemy
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
                Vector formationTarget = getFormationPosition(i, commander.getPosition(), commander.getVelocity());
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


    private Vector getFormationPosition(int minionIndex, Vector commanderPos, Vector commanderVelocity) {
        if (currentFormation == Formation.NORMAL) {
            return null; // Normal formation doesn't use target positions
        }
        
        double commanderAngle = commanderVelocity.magnitude() > 0 ? 
            Math.atan2(commanderVelocity.y, commanderVelocity.x) : 0;
        
        if (currentFormation == Formation.HEX_SHIELD) {
            return getHexShieldPosition(minionIndex, commanderPos, commanderAngle);
        } else if (currentFormation == Formation.ARROWHEAD) {
            return getArrowheadPosition(minionIndex, commanderPos, commanderAngle);
        } else if (currentFormation == Formation.PHALANX) {
            return getPhalanxPosition(minionIndex, commanderPos, commanderAngle);
        }
        
        return null;
    }
    
    private Vector getHexShieldPosition(int minionIndex, Vector commanderPos, double commanderAngle) {
        // Create a true hexagonal formation with offset rows
        double spacing = 50;
        
        // Determine row and column in hexagonal grid
        int row = 0;
        int boidCountBefore = 0;
        
        // Find which row this minion belongs to
        while (boidCountBefore + (row + 1) * 6 <= minionIndex) {
            boidCountBefore += (row + 1) * 6;
            row++;
        }
        
        int posInRow = minionIndex - boidCountBefore;
        int boidsInThisRow = (row + 1) * 6;
        
        // Calculate angle around the hexagon
        double angleStep = Math.PI * 2 / boidsInThisRow;
        double angle = commanderAngle + (angleStep * posInRow);
        
        // Distance from commander increases with row
        double distance = 70 + (row * 40);
        
        double x = commanderPos.x + Math.cos(angle) * distance;
        double y = commanderPos.y + Math.sin(angle) * distance;
        
        return new Vector(x, y);
    }
    
    private Vector getArrowheadPosition(int minionIndex, Vector commanderPos, double commanderAngle) {
        // Create an arrowhead formation where commander is tucked in middle-back
        // Tip extends far ahead, boids branch back in steep lines
        
        int row = 0;
        int boidCountBefore = 0;
        
        // Find which row this minion belongs to
        while (boidCountBefore + row + 1 <= minionIndex) {
            boidCountBefore += row + 1;
            row++;
        }
        
        int posInRow = minionIndex - boidCountBefore;
        int boidsInThisRow = row + 1;
        
        // Calculate total rows to determine commander's position
        int totalRows = 0;
        int tempCount = 0;
        while (tempCount < minions.size()) {
            tempCount += totalRows + 1;
            totalRows++;
        }
        totalRows = Math.max(1, totalRows - 1);
        
        // Position commander in middle-back (around 70% from tip)
        int commanderRow = Math.max(0, (int)(totalRows * 0.7));
        
        // Calculate depths: tip is furthest forward, rows get closer to commander
        double tipDepth = 180; // Tip 180 units ahead
        double rowSpacing = 50; // Spacing between rows
        double minionDepth = tipDepth - (row * rowSpacing);
        double commanderDepth = tipDepth - (commanderRow * rowSpacing);
        
        // Calculate lateral offset for this minion
        double lateralSpacing = 45;
        double lateralOffset = (posInRow - (boidsInThisRow - 1) / 2.0) * lateralSpacing;
        
        // Calculate position relative to commander
        // Commander stays at their current position, formation arranges around them
        double x = commanderPos.x + Math.cos(commanderAngle) * (minionDepth - commanderDepth);
        double y = commanderPos.y + Math.sin(commanderAngle) * (minionDepth - commanderDepth);
        
        // Add lateral offset perpendicular to facing direction
        double perpAngle = commanderAngle + Math.PI / 2;
        x += Math.cos(perpAngle) * lateralOffset;
        y += Math.sin(perpAngle) * lateralOffset;
        
        return new Vector(x, y);
    }
    
    private Vector getPhalanxPosition(int minionIndex, Vector commanderPos, double commanderAngle) {
        // Create a tight rectangular phalanx formation around the commander
        // Dense grid that rotates to face the commander's heading
        
        int totalMinions = minions.size();
        
        // Calculate optimal grid dimensions (try to make it as square as possible)
        int cols = (int) Math.ceil(Math.sqrt(totalMinions));
        int rows = (int) Math.ceil((double) totalMinions / cols);
        
        // Calculate which row and column this minion belongs to
        int row = minionIndex / cols;
        int col = minionIndex % cols;
        
        // Tight spacing for phalanx formation
        double spacing = 35; // Close formation
        
        // Center the formation around commander
        double centerRow = (rows - 1) / 2.0;
        double centerCol = (cols - 1) / 2.0;
        
        // Local formation offsets: forward/back and left/right
        double forwardOffset = (row - centerRow) * spacing;
        double lateralOffset = (col - centerCol) * spacing;
        
        // Rotate the grid so it faces commander movement direction
        double x = commanderPos.x + Math.cos(commanderAngle) * forwardOffset - Math.sin(commanderAngle) * lateralOffset;
        double y = commanderPos.y + Math.sin(commanderAngle) * forwardOffset + Math.cos(commanderAngle) * lateralOffset;
        
        return new Vector(x, y);
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
