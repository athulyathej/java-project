package main;

import entity.Player;
import entity.Projectile;
import tile.TileManager;


import javax.swing.JPanel;
import java.awt.*;
import entity.Enemy;
import java.util.ArrayList;
import java.util.Random;

public class GamePanel extends JPanel implements Runnable {

    //SCREEN SETTINGS
    public ArrayList<Projectile> projectiles = new ArrayList<>();
    final int originalTileSize = 16;
    final int scale = 3;

    public final int tileSize = originalTileSize * scale;
    public final int maxScreenCol = 16;
    public final int maxScreenRow = 12;
    public final int screenWidth = tileSize * maxScreenCol;
    public final int screenHeight = tileSize * maxScreenRow;

    // WORLD SETTINGS
    public final int maxWorldCol = 50;
    public final int maxWorldRow = 50;
    public final int worldWidth = tileSize * maxWorldCol;
    public final int worldHeight = tileSize * maxWorldRow;

    // FPS
    int FPS = 60;

    public TileManager tileM = new TileManager(this);
    public ArrayList<Enemy> enemies = new ArrayList<>();
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

        double drawInterval = 1000000000/FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;
        long timer = 0;
        int drawCount = 0;

        while (gameThread != null) {

            currentTime = System.nanoTime();

            delta += (currentTime - lastTime) / drawInterval;
            timer += (currentTime - lastTime);
            lastTime = currentTime;

            if(delta >= 1) {
                update();
                repaint();
                delta--;
                drawCount++;
            }

            if(timer >= 1000000000) {
                System.out.println("FPS:" + drawCount);
                drawCount = 0;
                timer = 0;
            }

        }
    }

    public void update() {
        player.update();

        // Check if this block was accidentally removed
        spawnTimer++;
        if(spawnTimer >= 180) { // Spawns every ~3 seconds
            spawnEnemy();
            spawnTimer = 0;
        }

        for (int i = 0; i < projectiles.size(); i++) {
            Projectile p = projectiles.get(i);
            if (p.alive) p.update();
            else { projectiles.remove(i); i--; }
        }

        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.alive) e.update();
            else { enemies.remove(i); i--; }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;

        tileM.draw(g2);
        player.draw(g2);

        // Make sure this enemy loop is still here
        for(int i = 0; i < enemies.size(); i++) {
            enemies.get(i).draw(g2);
        }

        // Projectile loop
        for (Projectile p : projectiles) {
            p.draw(g2);
        }

        g2.dispose();
    }
    public void spawnEnemy() {
        int maxAttempts = 10; // Prevent infinite loops if map is full
        for (int i = 0; i < maxAttempts; i++) {
            int randCol = random.nextInt(maxWorldCol);
            int randRow = random.nextInt(maxWorldRow);

            int tileNum = tileM.mapTileNum[randCol][randRow];

            if (!tileM.tile[tileNum].collision) {
                int colDiff = Math.abs(randCol - (player.worldX / tileSize));
                int rowDiff = Math.abs(randRow - (player.worldY / tileSize));

                // Ensure enemy spawns at least 6 tiles away from player
                if (colDiff > 6 || rowDiff > 6) {
                    enemies.add(new Enemy(this, randCol * tileSize, randRow * tileSize));
                    break;
                }
            }
        }
    }
}
