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

    // World Settings
    public final int maxWorldCol = 50;
    public final int maxWorldRow = 50;
    public final int worldWidth = tileSize * maxWorldCol;
    public final int worldHeight = tileSize * maxWorldRow;

    int FPS = 60;

    // Object Pools
    public ObjectPool<Projectile> projectilePool = new ObjectPool<>(() -> new Projectile(this), 30);
    public ObjectPool<Enemy> enemyPool = new ObjectPool<>(() -> new Enemy(this), 25);
    public ObjectPool<ExpGem> gemPool = new ObjectPool<>(() -> new ExpGem(this), 50);

    // Active Entities
    public ArrayList<Projectile> projectiles = new ArrayList<>();
    public ArrayList<Enemy> enemies = new ArrayList<>();
    public ArrayList<ExpGem> expGems = new ArrayList<>();

    public TileManager tileM = new TileManager(this);
    public int spawnTimer = 0;
    Random random = new Random();
    KeyHandler keyH = new KeyHandler();
    Thread gameThread;
    public CollisionChecker cChecker = new CollisionChecker(this);
    public Player player = new Player(this, keyH);

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
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
        long currentTime;

        while (gameThread != null) {
            currentTime = System.nanoTime();
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

        player.update();

        // Spawn Enemies
        spawnTimer++;
        if (spawnTimer >= 180) {
            spawnEnemy();
            spawnTimer = 0;
        }

        // Projectiles Update & Recycle
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

        // Enemies Update & Recycle
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.alive) {
                e.update();
            } else {
                // Drop pooled gem
                ExpGem gem = gemPool.obtain();
                gem.spawn(e.worldX, e.worldY);
                expGems.add(gem);

                enemies.remove(i);
                enemyPool.release(e);
                i--;
            }
        }

        // Gems Update & Recycle
        for (int i = 0; i < expGems.size(); i++) {
            ExpGem gem = expGems.get(i);
            if (gem.alive) {
                Rectangle gemHitbox = new Rectangle(gem.worldX, gem.worldY, gem.solidArea.width, gem.solidArea.height);
                Rectangle playerHitbox = new Rectangle(
                        player.worldX + player.solidArea.x,
                        player.worldY + player.solidArea.y,
                        player.solidArea.width,
                        player.solidArea.height
                );

                if (gemHitbox.intersects(playerHitbox)) {
                    player.gainExp(gem.expValue);
                    gem.alive = false;
                }
            } else {
                expGems.remove(i);
                gemPool.release(gem);
                i--;
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        tileM.draw(g2);

        for (ExpGem gem : expGems) gem.draw(g2);
        for (Enemy enemy : enemies) enemy.draw(g2);
        for (Projectile p : projectiles) p.draw(g2);
        player.draw(g2);

        // Player Health HUD & Game Over Screen
        drawHUD(g2);

        g2.dispose();
    }

    private void drawHUD(Graphics2D g2) {
        // Health Bar UI
        int barWidth = 150;
        int barHeight = 16;
        int x = 20;
        int y = 20;

        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(x, y, barWidth, barHeight);

        g2.setColor(Color.GREEN);
        int currentHpWidth = (int) (((double) player.life / player.maxLife) * barWidth);
        g2.fillRect(x, y, Math.max(0, currentHpWidth), barHeight);

        g2.setColor(Color.WHITE);
        g2.drawRect(x, y, barWidth, barHeight);
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString("HP: " + player.life + "/" + player.maxLife, x + 10, y + 13);

        // Game Over message
        if (!player.alive) {
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, 0, screenWidth, screenHeight);
            g2.setColor(Color.RED);
            g2.setFont(new Font("Arial", Font.BOLD, 48));
            g2.drawString("GAME OVER", screenWidth / 2 - 150, screenHeight / 2);
        }
    }

    public void spawnEnemy() {
        int maxAttempts = 10;
        for (int i = 0; i < maxAttempts; i++) {
            int randCol = random.nextInt(maxWorldCol);
            int randRow = random.nextInt(maxWorldRow);
            int tileNum = tileM.mapTileNum[randCol][randRow];

            if (!tileM.tile[tileNum].collision) {
                int colDiff = Math.abs(randCol - (player.worldX / tileSize));
                int rowDiff = Math.abs(randRow - (player.worldY / tileSize));

                if (colDiff > 6 || rowDiff > 6) {
                    Enemy e = enemyPool.obtain();
                    e.spawn(randCol * tileSize, randRow * tileSize);
                    enemies.add(e);
                    break;
                }
            }
        }
    }
}