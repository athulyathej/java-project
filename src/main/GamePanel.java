
package main;

import entity.Enemy;
import entity.ExpGem;
import entity.Player;
import entity.Projectile;
import tile.TileManager;

import javax.swing.JPanel;
import java.awt.*;
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
    public static final int PLAY_STATE = 1;
    public static final int LEVELUP_STATE = 2;
    public int gameState = PLAY_STATE;

    // World Settings
    public final int maxWorldCol = 50;
    public final int maxWorldRow = 50;
    public final int worldWidth = tileSize * maxWorldCol;
    public final int worldHeight = tileSize * maxWorldRow;

    int FPS = 60;

    // Object Pools — already implemented in your project
    public ObjectPool<Projectile> projectilePool =
            new ObjectPool<>(() -> new Projectile(this), 30);

    public ObjectPool<Enemy> enemyPool =
            new ObjectPool<>(() -> new Enemy(this), 25);

    public ObjectPool<ExpGem> gemPool =
            new ObjectPool<>(() -> new ExpGem(this), 50);

    // Active Entities
    public ArrayList<Projectile> projectiles = new ArrayList<>();
    public ArrayList<Enemy> enemies = new ArrayList<>();
    public ArrayList<ExpGem> expGems = new ArrayList<>();

    public TileManager tileM = new TileManager(this);
    public int spawnTimer = 0;
    Random random = new Random();

    public KeyHandler keyH = new KeyHandler(this);
    Thread gameThread;

    public CollisionChecker cChecker = new CollisionChecker(this);
    public Player player = new Player(this, keyH);

    public GamePanel() {
        this.setPreferredSize(
                new Dimension(screenWidth, screenHeight)
        );
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);
        this.setFocusable(true);
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

    public void update() {
        if (!player.alive) return;

        // Pause gameplay while choosing an upgrade
        if (gameState == LEVELUP_STATE) {
            return;
        }

        player.update();

        // Spawn enemies
        spawnTimer++;

        if (spawnTimer >= 180) {
            spawnEnemy();
            spawnTimer = 0;
        }

        // Update and recycle projectiles
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

        // Update enemies and drop EXP gems
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);

            if (e.alive) {
                e.update();
            } else {
                ExpGem gem = gemPool.obtain();
                gem.spawn(e.worldX, e.worldY);
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
                        gem.worldX,
                        gem.worldY,
                        gem.solidArea.width,
                        gem.solidArea.height
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

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

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

        if (gameState == LEVELUP_STATE) {
            drawUpgradeGUI(g2);
        }

        g2.dispose();
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

        int currentHpWidth = (int) (
                ((double) player.life / player.maxLife) * barWidth
        );

        g2.fillRect(
                x, y,
                Math.max(0, currentHpWidth),
                barHeight
        );

        g2.setColor(Color.WHITE);
        g2.drawRect(x, y, barWidth, barHeight);
        g2.setFont(new Font("Arial", Font.BOLD, 12));

        g2.drawString(
                "HP: " + player.life + "/" + player.maxLife,
                x + 10, y + 13
        );

        // EXP bar
        int expY = y + 22;

        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(x, expY, barWidth, 10);

        g2.setColor(Color.CYAN);

        int currentExpWidth = (int) (
                ((double) player.exp / player.nextLevelExp)
                        * barWidth
        );

        g2.fillRect(
                x, expY,
                Math.max(0, currentExpWidth),
                10
        );

        g2.setColor(Color.WHITE);
        g2.drawRect(x, expY, barWidth, 10);

        g2.drawString(
                "LVL " + player.level,
                x + barWidth + 8, y + 22
        );

        // Game over screen
        if (!player.alive) {
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, 0, screenWidth, screenHeight);

            g2.setColor(Color.RED);
            g2.setFont(new Font("Arial", Font.BOLD, 48));

            g2.drawString(
                    "GAME OVER",
                    screenWidth / 2 - 150,
                    screenHeight / 2
            );
        }
    }

    private void drawUpgradeGUI(Graphics2D g2) {

        // Dim background
        g2.setColor(new Color(0, 0, 0, 190));
        g2.fillRect(0, 0, screenWidth, screenHeight);

        // Main frame
        int frameX = tileSize * 2;
        int frameY = tileSize;
        int frameW = screenWidth - (tileSize * 4);
        int frameH = screenHeight - (tileSize * 2);

        g2.setColor(new Color(30, 30, 45, 240));
        g2.fillRoundRect(
                frameX, frameY, frameW, frameH, 25, 25
        );

        g2.setColor(new Color(255, 215, 0));
        g2.setStroke(new BasicStroke(3));

        g2.drawRoundRect(
                frameX, frameY, frameW, frameH, 25, 25
        );

        // Header
        g2.setFont(new Font("Arial", Font.BOLD, 28));
        g2.setColor(Color.YELLOW);

        g2.drawString(
                "LEVEL UP!",
                frameX + (frameW / 2) - 80,
                frameY + 45
        );

        g2.setFont(new Font("Arial", Font.PLAIN, 14));
        g2.setColor(Color.LIGHT_GRAY);

        g2.drawString(
                "Press 1, 2, 3, or 4 to pick an upgrade:",
                frameX + 40,
                frameY + 75
        );

        // Upgrade titles
        String[] titles = {
                "[1] DAMAGE +1",
                "[2] ATTACK SPEED +15%",
                "[3] MAX HEALTH +2",
                "[4] RANGE +40px"
        };

        // Current stats
        String[] descriptions = {
                "Current DMG: " + player.attackDamage,
                "Fire Rate: " + String.format(
                        "%.2f",
                        60.0 / player.attackCooldownMax
                ) + " shots/sec",
                "Max HP: " + player.maxLife,
                "Acquisition Range: "
                        + (int) player.attackRange
        };

        int cardX = frameX + 30;
        int cardW = frameW - 60;
        int cardH = 50;
        int startY = frameY + 105;

        for (int i = 0; i < 4; i++) {
            int cardY = startY + (i * (cardH + 12));

            // Card panel
            g2.setColor(new Color(60, 60, 80));

            g2.fillRoundRect(
                    cardX, cardY, cardW, cardH, 12, 12
            );

            g2.setColor(Color.WHITE);

            g2.drawRoundRect(
                    cardX, cardY, cardW, cardH, 12, 12
            );

            // Card text
            g2.setFont(new Font("Arial", Font.BOLD, 16));
            g2.setColor(new Color(255, 220, 100));

            g2.drawString(
                    titles[i],
                    cardX + 15,
                    cardY + 24
            );

            g2.setFont(new Font("Arial", Font.ITALIC, 12));
            g2.setColor(Color.LIGHT_GRAY);

            g2.drawString(
                    descriptions[i],
                    cardX + 15,
                    cardY + 42
            );
        }
    }

    public void spawnEnemy() {
        int maxAttempts = 10;

        for (int i = 0; i < maxAttempts; i++) {
            int randCol = random.nextInt(maxWorldCol);
            int randRow = random.nextInt(maxWorldRow);

            int tileNum = tileM.mapTileNum[randCol][randRow];

            if (!tileM.tile[tileNum].collision) {
                int colDiff = Math.abs(
                        randCol - (player.worldX / tileSize)
                );

                int rowDiff = Math.abs(
                        randRow - (player.worldY / tileSize)
                );

                if (colDiff > 6 || rowDiff > 6) {
                    Enemy e = enemyPool.obtain();

                    e.spawn(
                            randCol * tileSize,
                            randRow * tileSize
                    );

                    enemies.add(e);
                    break;
                }
            }
        }
    }
}
