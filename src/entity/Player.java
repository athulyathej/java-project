package entity;

import main.GamePanel;
import main.KeyHandler;
import main.UtilityTool;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Player extends Entity {

    public int shootCooldown = 0;
    GamePanel gp;
    KeyHandler keyH;

    // Health & Invincibility
    public boolean invincible = false;
    public int invincibleCounter = 0;

    // EXP and Level
    public int level = 1;
    public int exp = 0;
    public int nextLevelExp = 50;

    public final int screenX;
    public final int screenY;

    public Player(GamePanel gp, KeyHandler keyH) {
        this.gp = gp;
        this.keyH = keyH;

        screenX = gp.screenWidth / 2 - (gp.tileSize / 2);
        screenY = gp.screenHeight / 2 - (gp.tileSize / 2);

        solidArea = new Rectangle(12, 16, 32, 32);

        setDefaultValues();
        getPlayerImage();
    }

    public void setDefaultValues() {
        worldX = gp.tileSize * 23;
        worldY = gp.tileSize * 21;
        speed = 4;
        direction = "down";
        maxLife = 6; // 6 HP points (e.g., 3 hearts at 2 HP each)
        life = maxLife;
    }

    public void getPlayerImage() {
        up1 = setup("boy_up_1");
        up2 = setup("boy_up_2");
        down1 = setup("boy_down_1");
        down2 = setup("boy_down_2");
        left1 = setup("boy_left_1");
        left2 = setup("boy_left_2");
        right1 = setup("boy_right_1");
        right2 = setup("boy_right_2");
    }

    public BufferedImage setup(String imageName) {
        UtilityTool uTool = new UtilityTool();
        BufferedImage image = null;
        try {
            image = ImageIO.read(getClass().getResourceAsStream("/Player/" + imageName + ".png"));
            image = uTool.scaleImage(image, gp.tileSize, gp.tileSize);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return image;
    }

    public void update() {
        if (keyH.upPressed || keyH.downPressed || keyH.leftPressed || keyH.rightPressed) {
            if (keyH.upPressed) direction = "up";
            else if (keyH.downPressed) direction = "down";
            else if (keyH.leftPressed) direction = "left";
            else if (keyH.rightPressed) direction = "right";

            collisionOn = false;
            gp.cChecker.checkTile(this);

            if (!collisionOn) {
                switch (direction) {
                    case "up": worldY -= speed; break;
                    case "down": worldY += speed; break;
                    case "left": worldX -= speed; break;
                    case "right": worldX += speed; break;
                }
            }

            spriteCounter++;
            if (spriteCounter > 10) {
                spriteNum = (spriteNum == 1) ? 2 : 1;
                spriteCounter = 0;
            }
        }

        // Invincibility cooldown (1 sec / 60 frames)
        if (invincible) {
            invincibleCounter++;
            if (invincibleCounter > 60) {
                invincible = false;
                invincibleCounter = 0;
            }
        }

        // Projectile Shooting from Object Pool
        shootCooldown++;
        if (shootCooldown >= 60) {
            Enemy closestEnemy = null;
            double closestDist = 200;

            for (Enemy enemy : gp.enemies) {
                if (!enemy.alive) continue;
                double dist = Math.sqrt(Math.pow(enemy.worldX - worldX, 2) + Math.pow(enemy.worldY - worldY, 2));
                if (dist < closestDist) {
                    closestDist = dist;
                    closestEnemy = enemy;
                }
            }

            if (closestEnemy != null) {
                Projectile p = gp.projectilePool.obtain();
                p.spawn(worldX, worldY, closestEnemy);
                gp.projectiles.add(p);
                shootCooldown = 0;
            }
        }
    }

    public void takeDamage(int damage) {
        if (!invincible) {
            life -= damage;
            invincible = true;
            if (life <= 0) {
                life = 0;
                alive = false;
                System.out.println("Game Over!");
            }
        }
    }

    public void draw(Graphics2D g2) {
        BufferedImage image = null;
        switch (direction) {
            case "up": image = (spriteNum == 1) ? up1 : up2; break;
            case "down": image = (spriteNum == 1) ? down1 : down2; break;
            case "left": image = (spriteNum == 1) ? left1 : left2; break;
            case "right": image = (spriteNum == 1) ? right1 : right2; break;
        }

        // Flash player when invincible
        if (invincible && (invincibleCounter / 6) % 2 == 0) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
        }

        g2.drawImage(image, screenX, screenY, null);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    public void gainExp(int amount) {
        exp += amount;
        if (exp >= nextLevelExp) {
            level++;
            exp -= nextLevelExp;
            nextLevelExp = (int) (nextLevelExp * 1.5);
            System.out.println("Level Up! Current Level: " + level);
        }
    }
}