
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

    // Upgradable Combat Stats
    public int attackDamage = 1;
    public int attackCooldownMax = 60;
    public double attackRange = 200.0;

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

        maxLife = 6;
        life = maxLife;

        attackDamage = 1;
        attackCooldownMax = 60;
        attackRange = 200.0;
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
            image = ImageIO.read(
                getClass().getResourceAsStream(
                    "/Player/" + imageName + ".png"
                )
            );

            image = uTool.scaleImage(
                image, gp.tileSize, gp.tileSize
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return image;
    }

    public void update() {

        // Player movement
        if (keyH.upPressed || keyH.downPressed
                || keyH.leftPressed || keyH.rightPressed) {

            if (keyH.upPressed) direction = "up";
            else if (keyH.downPressed) direction = "down";
            else if (keyH.leftPressed) direction = "left";
            else if (keyH.rightPressed) direction = "right";

            collisionOn = false;
            gp.cChecker.checkTile(this);

            if (!collisionOn) {
                switch (direction) {
                    case "up":
                        worldY -= speed;
                        break;
                    case "down":
                        worldY += speed;
                        break;
                    case "left":
                        worldX -= speed;
                        break;
                    case "right":
                        worldX += speed;
                        break;
                }
            }

            // Sprite animation
            spriteCounter++;

            if (spriteCounter > 10) {
                spriteNum = (spriteNum == 1) ? 2 : 1;
                spriteCounter = 0;
            }
        }

        // Invincibility cooldown
        if (invincible) {
            invincibleCounter++;

            if (invincibleCounter > 60) {
                invincible = false;
                invincibleCounter = 0;
            }
        }

        // Automatic projectile shooting
        shootCooldown++;

        if (shootCooldown >= attackCooldownMax) {

            Enemy closestEnemy = null;
            double closestDist = attackRange;

            for (Enemy enemy : gp.enemies) {
                if (!enemy.alive) continue;

                double dist = Math.sqrt(
                    Math.pow(enemy.worldX - worldX, 2)
                    + Math.pow(enemy.worldY - worldY, 2)
                );

                if (dist < closestDist) {
                    closestDist = dist;
                    closestEnemy = enemy;
                }
            }

            if (closestEnemy != null) {
                Projectile p = gp.projectilePool.obtain();

                p.spawn(
                    worldX,
                    worldY,
                    closestEnemy,
                    attackDamage
                );

                gp.projectiles.add(p);
                shootCooldown = 0;
            }
        }
    }

    // Apply damage to the player
    public void takeDamage(int damage) {
        if (!invincible) {
            life -= damage;
            invincible = true;

            if (life <= 0) {
                life = 0;
                alive = false;
            }
        }
    }

    // Apply the selected level-up upgrade
    public void applyUpgrade(int optionIndex) {
        switch (optionIndex) {

            case 1:
                // Damage +1
                attackDamage += 1;
                break;

            case 2:
                // Attack speed: reduce cooldown by 15%
                attackCooldownMax = Math.max(
                    10,
                    (int) (attackCooldownMax * 0.85)
                );
                break;

            case 3:
                // Maximum health +2 and heal 2 HP
                maxLife += 2;
                life = Math.min(life + 2, maxLife);
                break;

            case 4:
                // Attack range +40 pixels
                attackRange += 40.0;
                break;
        }
    }

    public void draw(Graphics2D g2) {
        BufferedImage image = null;

        switch (direction) {
            case "up":
                image = (spriteNum == 1) ? up1 : up2;
                break;
            case "down":
                image = (spriteNum == 1) ? down1 : down2;
                break;
            case "left":
                image = (spriteNum == 1) ? left1 : left2;
                break;
            case "right":
                image = (spriteNum == 1) ? right1 : right2;
                break;
        }

        // Flash player when invincible
        if (invincible && (invincibleCounter / 6) % 2 == 0) {
            g2.setComposite(
                AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 0.4f
                )
            );
        }

        g2.drawImage(image, screenX, screenY, null);

        g2.setComposite(
            AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, 1.0f
            )
        );
    }

    // Collect EXP and trigger level-up
    public void gainExp(int amount) {
        exp += amount;

        if (exp >= nextLevelExp) {
            level++;
            exp -= nextLevelExp;

            nextLevelExp = (int) (nextLevelExp * 1.5);

            gp.gameState = GamePanel.LEVELUP_STATE;
        }
    }
}
