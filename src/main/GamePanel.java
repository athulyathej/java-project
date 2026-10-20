package main;

import entity.Enemy;
import entity.ExpGem;
import entity.Player;
import entity.Projectile;
import tile.TileManager;

import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Random;

public class GamePanel extends JPanel implements Runnable {

    // Screen Settings
    final int originalTileSize = 16;
    final int scale = 3;
    public final int tileSize = originalTileSize * scale;
    public final int maxScreenCol = 16;
    public final int maxScreenRow = 12;
    public final int screenWidth = tileSize * maxScreenCol;
    public final int screenHeight = tileSize * maxScreenRow;

    // Game States
    public static final int TITLE_STATE = 0;
    public static final int PLAY_STATE = 1;
    public static final int LEVELUP_STATE = 2;
    public static final int GAMEOVER_STATE = 3;
    public int gameState = TITLE_STATE;

    // World Settings
    public final int maxWorldCol = 50;
    public final int maxWorldRow = 50;
    public final int worldWidth = tileSize * maxWorldCol;
    public final int worldHeight = tileSize * maxWorldRow;

    int FPS = 60;

    // Object Pools
    public ObjectPool<Projectile> projectilePool =
            new ObjectPool<>(() -> new Projectile(this), 50);

    public ObjectPool<Enemy> enemyPool =
            new ObjectPool<>(() -> new Enemy(this), 120);

    public ObjectPool<ExpGem> gemPool =
            new ObjectPool<>(() -> new ExpGem(this), 120);

    // Active Entities
    public ArrayList<Projectile> projectiles = new ArrayList<>();
    public ArrayList<Enemy> enemies = new ArrayList<>();
    public ArrayList<ExpGem> expGems = new ArrayList<>();

    public TileManager tileM = new TileManager(this);
    Random random = new Random();

    public KeyHandler keyH = new KeyHandler(this);
    Thread gameThread;

    public CollisionChecker cChecker = new CollisionChecker(this);
    public Player player = new Player(this, keyH);

    // Wave Management & High Score
    public int currentWave = 1;
    public int highScoreWave = 1;
    public int enemiesToSpawn = 6;
    public boolean waveInProgress = false;
    public int waveBreakTimer = 0;
    public final int waveBreakDuration = 180; // 3 seconds at 60 FPS

    // Clickable Button Bounds
    public Rectangle startButton = new Rectangle(screenWidth / 2 - 90, screenHeight / 2 + 60, 180, 50);
    public Rectangle retryButton = new Rectangle(screenWidth / 2 - 90, screenHeight / 2 + 60, 180, 50);

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);
        this.setFocusable(true);

        // Mouse click listener for UI Buttons
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();
                if (gameState == TITLE_STATE && startButton.contains(p)) {
                    startGame();
                } else if (gameState == GAMEOVER_STATE && retryButton.contains(p)) {
                    restartGame();
                }
            }
        });
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1000000000.0 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();

        while (gameThread != null) {
            long currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    public void startGame() {
        resetGameEntities();
        currentWave = 1;
        enemiesToSpawn = 6;
        waveInProgress = true;
        gameState = PLAY_STATE;
        spawnWaveInstantly();
    }

    public void restartGame() {
        startGame();
    }

    private void resetGameEntities() {
        // Clear active entities back to pools
        for (Projectile p : projectiles) projectilePool.release(p);
        projectiles.clear();

        for (Enemy e : enemies) enemyPool.release(e);
        enemies.clear();

        for (ExpGem g : expGems) gemPool.release(g);
        expGems.clear();

        // Reset player stats & position
        player.setDefaultValues();
        player.alive = true;
        player.level = 1;
        player.exp = 0;
        player.nextLevelExp = 50;
    }

    public void update() {
        if (gameState != PLAY_STATE) return;

        // Check if player died
        if (!player.alive) {
            if (currentWave > highScoreWave) {
                highScoreWave = currentWave;
            }
            gameState = GAMEOVER_STATE;
            return;
        }

        player.update();

        // Wave Spawning & Progression Logic
        if (waveInProgress) {
            if (enemies.isEmpty()) {
                // Wave cleared!
                waveInProgress = false;
                waveBreakTimer = waveBreakDuration;

                if (currentWave > highScoreWave) {
                    highScoreWave = currentWave;
                }
            }
        } else {
            // Wave countdown between waves
            waveBreakTimer--;
            if (waveBreakTimer <= 0) {
                startNextWave();
            }
        }

        // Update Projectiles
        for (int i = 0; i < projectiles.size(); i++) {
            Projectile p = projectiles.get(i);
            if (p.alive) {
                p.update();
            } else {
                projectiles.remove(i);
                projectilePool.release(p);
                i--;
            }
        }

        // Update Enemies & drop gems
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.alive) {
                e.update();
            } else {
                ExpGem gem = gemPool.obtain();
                gem.spawn(e.worldX, e.worldY, e.expReward);
                expGems.add(gem);

                enemies.remove(i);
                enemyPool.release(e);
                i--;
            }
        }

        // Collect EXP gems
        for (int i = 0; i < expGems.size(); i++) {
            ExpGem gem = expGems.get(i);
            if (gem.alive) {
                Rectangle gemHitbox = new Rectangle(
                        gem.worldX, gem.worldY,
                        gem.solidArea.width, gem.solidArea.height
                );

                Rectangle playerHitbox = new Rectangle(
                        player.worldX + player.solidArea.x,
                        player.worldY + player.solidArea.y,
                        player.solidArea.width,
                        player.solidArea.height
                );

                if (gemHitbox.intersects(playerHitbox)) {
                    gem.alive = false;
                    player.gainExp(gem.expValue);
                }
            } else {
                expGems.remove(i);
                gemPool.release(gem);
                i--;
            }
        }
    }

    private void startNextWave() {
        currentWave++;
        enemiesToSpawn = 5 + (currentWave * 4);
        waveInProgress = true;
        spawnWaveInstantly();
    }

    // Spawns all enemies for the wave at the exact same moment
    private void spawnWaveInstantly() {
        for (int i = 0; i < enemiesToSpawn; i++) {
            Enemy.EnemyType typeToSpawn = Enemy.EnemyType.NORMAL;
            int roll = random.nextInt(100);

            if (currentWave >= 5 && roll < 10) {
                typeToSpawn = Enemy.EnemyType.BOSS;
            } else if (currentWave >= 3 && roll < 35) {
                typeToSpawn = Enemy.EnemyType.TANK;
            } else if (currentWave >= 2 && roll < 65) {
                typeToSpawn = Enemy.EnemyType.FAST;
            }

            spawnEnemy(typeToSpawn);
        }
    }

    public void spawnEnemy(Enemy.EnemyType type) {
        int maxAttempts = 20;

        for (int i = 0; i < maxAttempts; i++) {
            int randCol = random.nextInt(maxWorldCol);
            int randRow = random.nextInt(maxWorldRow);

            int tileNum = tileM.mapTileNum[randCol][randRow];

            if (!tileM.tile[tileNum].collision) {
                int colDiff = Math.abs(randCol - (player.worldX / tileSize));
                int rowDiff = Math.abs(randRow - (player.worldY / tileSize));

                // Spawn away from the player
                if (colDiff > 6 || rowDiff > 6) {
                    Enemy e = enemyPool.obtain();
                    e.spawn(randCol * tileSize, randRow * tileSize, type, currentWave);
                    enemies.add(e);
                    break;
                }
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        if (gameState == TITLE_STATE) {
            drawTitleScreen(g2);
        } else {
            tileM.draw(g2);

            for (ExpGem gem : expGems) {
                gem.draw(g2);
            }

            for (Enemy enemy : enemies) {
                enemy.draw(g2);
            }

            for (Projectile p : projectiles) {
                p.draw(g2);
            }

            player.draw(g2);

            drawHUD(g2);
            drawWaveUI(g2);

            if (gameState == LEVELUP_STATE) {
                drawUpgradeGUI(g2);
            } else if (gameState == GAMEOVER_STATE) {
                drawGameOverScreen(g2);
            }
        }

        g2.dispose();
    }

    private void drawTitleScreen(Graphics2D g2) {
        // Background
        g2.setColor(new Color(20, 20, 25));
        g2.fillRect(0, 0, screenWidth, screenHeight);

        // Title
        g2.setFont(new Font("Arial", Font.BOLD, 42));
        g2.setColor(new Color(255, 215, 0));
        String title = "ROGUE HORDE";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, (screenWidth - fm.stringWidth(title)) / 2, screenHeight / 3);

        // High Score
        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        g2.setColor(Color.LIGHT_GRAY);
        String hsText = "BEST WAVE: " + highScoreWave;
        fm = g2.getFontMetrics();
        g2.drawString(hsText, (screenWidth - fm.stringWidth(hsText)) / 2, screenHeight / 3 + 45);

        // Start Button
        drawButton(g2, startButton, "PLAY", new Color(46, 204, 113));
    }

    private void drawGameOverScreen(Graphics2D g2) {
        // Semi-transparent overlay
        g2.setColor(new Color(0, 0, 0, 190));
        g2.fillRect(0, 0, screenWidth, screenHeight);

        // Game Over Label
        g2.setColor(new Color(231, 76, 60));
        g2.setFont(new Font("Arial", Font.BOLD, 48));
        String text = "GAME OVER";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (screenWidth - fm.stringWidth(text)) / 2, screenHeight / 3);

        // Wave Reached & High Score
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.setColor(Color.WHITE);
        String waveReached = "Wave Reached: " + currentWave;
        fm = g2.getFontMetrics();
        g2.drawString(waveReached, (screenWidth - fm.stringWidth(waveReached)) / 2, screenHeight / 3 + 45);

        g2.setColor(new Color(255, 215, 0));
        String hs = "High Score Wave: " + highScoreWave;
        fm = g2.getFontMetrics();
        g2.drawString(hs, (screenWidth - fm.stringWidth(hs)) / 2, screenHeight / 3 + 75);

        // Retry Button
        drawButton(g2, retryButton, "RETRY", new Color(52, 152, 219));
    }

    private void drawButton(Graphics2D g2, Rectangle rect, String text, Color baseColor) {
        g2.setColor(baseColor);
        g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 12, 12);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 12, 12);

        g2.setFont(new Font("Arial", Font.BOLD, 20));
        FontMetrics fm = g2.getFontMetrics();
        int tx = rect.x + (rect.width - fm.stringWidth(text)) / 2;
        int ty = rect.y + ((rect.height - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(text, tx, ty);
    }

    private void drawWaveUI(Graphics2D g2) {
        int boxWidth = 160;
        int boxHeight = 55;
        int x = screenWidth - boxWidth - 20;
        int y = 20;

        g2.setColor(new Color(20, 20, 30, 200));
        g2.fillRoundRect(x, y, boxWidth, boxHeight, 10, 10);
        g2.setColor(new Color(255, 215, 0));
        g2.drawRoundRect(x, y, boxWidth, boxHeight, 10, 10);

        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.setColor(Color.WHITE);
        g2.drawString("WAVE " + currentWave, x + 15, y + 24);

        g2.setFont(new Font("Arial", Font.PLAIN, 13));
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawString("Enemies Left: " + enemies.size(), x + 15, y + 44);

        if (!waveInProgress && gameState == PLAY_STATE) {
            int secondsLeft = (waveBreakTimer / FPS) + 1;
            String text = "WAVE " + currentWave + " CLEARED!";
            String countdown = "Next Wave in " + secondsLeft + "...";

            g2.setFont(new Font("Arial", Font.BOLD, 26));
            g2.setColor(new Color(255, 215, 0));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (screenWidth - fm.stringWidth(text)) / 2, screenHeight / 4);

            g2.setFont(new Font("Arial", Font.PLAIN, 18));
            g2.setColor(Color.WHITE);
            FontMetrics fm2 = g2.getFontMetrics();
            g2.drawString(countdown, (screenWidth - fm2.stringWidth(countdown)) / 2, screenHeight / 4 + 30);
        }
    }

    private void drawHUD(Graphics2D g2) {
        int barWidth = 150;
        int barHeight = 16;
        int x = 20;
        int y = 20;

        // Health bar
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(x, y, barWidth, barHeight);

        g2.setColor(Color.GREEN);
        int currentHpWidth = (int) (((double) player.life / player.maxLife) * barWidth);
        g2.fillRect(x, y, Math.max(0, currentHpWidth), barHeight);

        g2.setColor(Color.WHITE);
        g2.drawRect(x, y, barWidth, barHeight);
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString("HP: " + player.life + "/" + player.maxLife, x + 10, y + 13);

        // EXP bar
        int expY = y + 22;
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(x, expY, barWidth, 10);

        g2.setColor(Color.CYAN);
        int currentExpWidth = (int) (((double) player.exp / player.nextLevelExp) * barWidth);
        g2.fillRect(x, expY, Math.max(0, currentExpWidth), 10);

        g2.setColor(Color.WHITE);
        g2.drawRect(x, expY, barWidth, 10);
        g2.drawString("LVL " + player.level, x + barWidth + 8, y + 22);
    }

    private void drawUpgradeGUI(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 190));
        g2.fillRect(0, 0, screenWidth, screenHeight);

        int frameX = tileSize * 2;
        int frameY = tileSize;
        int frameW = screenWidth - (tileSize * 4);
        int frameH = screenHeight - (tileSize * 2);

        g2.setColor(new Color(30, 30, 45, 240));
        g2.fillRoundRect(frameX, frameY, frameW, frameH, 25, 25);

        g2.setColor(new Color(255, 215, 0));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(frameX, frameY, frameW, frameH, 25, 25);

        g2.setFont(new Font("Arial", Font.BOLD, 28));
        g2.setColor(Color.YELLOW);
        g2.drawString("LEVEL UP!", frameX + (frameW / 2) - 80, frameY + 45);

        g2.setFont(new Font("Arial", Font.PLAIN, 14));
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawString("Press 1, 2, 3, or 4 to pick an upgrade:", frameX + 40, frameY + 75);

        String[] titles = {
                "[1] DAMAGE +1",
                "[2] ATTACK SPEED +15%",
                "[3] MAX HEALTH +2",
                "[4] RANGE +40px"
        };

        String[] descriptions = {
                "Current DMG: " + player.attackDamage,
                "Fire Rate: " + String.format("%.2f", 60.0 / player.attackCooldownMax) + " shots/sec",
                "Max HP: " + player.maxLife,
                "Acquisition Range: " + (int) player.attackRange
        };

        int cardX = frameX + 30;
        int cardW = frameW - 60;
        int cardH = 50;
        int startY = frameY + 105;

        for (int i = 0; i < 4; i++) {
            int cardY = startY + (i * (cardH + 12));
            g2.setColor(new Color(60, 60, 80));
            g2.fillRoundRect(cardX, cardY, cardW, cardH, 12, 12);

            g2.setColor(Color.WHITE);
            g2.drawRoundRect(cardX, cardY, cardW, cardH, 12, 12);

            g2.setFont(new Font("Arial", Font.BOLD, 16));
            g2.setColor(new Color(255, 220, 100));
            g2.drawString(titles[i], cardX + 15, cardY + 24);

            g2.setFont(new Font("Arial", Font.ITALIC, 12));
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawString(descriptions[i], cardX + 15, cardY + 42);
        }
    }
}