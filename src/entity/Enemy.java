package entity;

import main.GamePanel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Enemy extends Entity {

    GamePanel gp;

    public enum EnemyType {
        NORMAL, FAST, TANK, BOSS
    }

    public EnemyType type = EnemyType.NORMAL;
    public int damage = 1;
    public int expReward = 10;
    private Color color = Color.RED;

    public Enemy(GamePanel gp) {
        this.gp = gp;
        this.solidArea = new Rectangle(12, 16, 24, 24);
    }

    public void spawn(int worldX, int worldY, EnemyType type, int waveNumber) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.type = type;
        this.direction = "down";
        this.alive = true;
        this.collisionOn = false;

        // Base wave scaling factor
        int bonusHp = (waveNumber - 1) * 2;

        switch (type) {
            case FAST:
                this.speed = 3;
                this.maxLife = 2 + (waveNumber / 2);
                this.damage = 1;
                this.expReward = 15;
                this.color = new Color(255, 140, 0); // Orange
                break;
            case TANK:
                this.speed = 1;
                this.maxLife = 8 + bonusHp * 2;
                this.damage = 2;
                this.expReward = 25;
                this.color = new Color(138, 43, 226); // Purple
                break;
            case BOSS:
                this.speed = 1;
                this.maxLife = 20 + bonusHp * 4;
                this.damage = 3;
                this.expReward = 50;
                this.color = new Color(178, 34, 34); // Crimson
                break;
            case NORMAL:
            default:
                this.speed = 2;
                this.maxLife = 3 + bonusHp;
                this.damage = 1;
                this.expReward = 10;
                this.color = Color.RED;
                break;
        }

        this.life = this.maxLife;
    }

    public void update() {
        int xDiff = gp.player.worldX - worldX;
        int yDiff = gp.player.worldY - worldY;

        String primary = "", secondary = "";
        if (Math.abs(xDiff) > Math.abs(yDiff)) {
            primary = (xDiff > 0) ? "right" : "left";
            secondary = (yDiff > 0) ? "down" : "up";
        } else {
            primary = (yDiff > 0) ? "down" : "up";
            secondary = (xDiff > 0) ? "right" : "left";
        }

        direction = primary;
        collisionOn = false;
        gp.cChecker.checkTile(this);

        if (collisionOn) {
            direction = secondary;
            collisionOn = false;
            gp.cChecker.checkTile(this);
        }

        if (!collisionOn) {
            switch (direction) {
                case "up": worldY -= speed; break;
                case "down": worldY += speed; break;
                case "left": worldX -= speed; break;
                case "right": worldX += speed; break;
            }
        }

        checkPlayerCollision();
    }

    private void checkPlayerCollision() {
        Rectangle enemyHitbox = new Rectangle(worldX + solidArea.x, worldY + solidArea.y, solidArea.width, solidArea.height);
        Rectangle playerHitbox = new Rectangle(gp.player.worldX + gp.player.solidArea.x, gp.player.worldY + gp.player.solidArea.y, gp.player.solidArea.width, gp.player.solidArea.height);

        if (enemyHitbox.intersects(playerHitbox)) {
            gp.player.takeDamage(damage);
        }
    }

    public void draw(Graphics2D g2) {
        int screenX = worldX - gp.player.worldX + gp.player.screenX;
        int screenY = worldY - gp.player.worldY + gp.player.screenY;

        if (worldX + gp.tileSize > gp.player.worldX - gp.player.screenX &&
                worldX - gp.tileSize < gp.player.worldX + gp.player.screenX &&
                worldY + gp.tileSize > gp.player.worldY - gp.player.screenY &&
                worldY - gp.tileSize < gp.player.worldY + gp.player.screenY) {

            // Draw Enemy Body
            g2.setColor(color);
            g2.fillRect(screenX, screenY, gp.tileSize, gp.tileSize);

            // Draw Mini Health Bar over tougher enemies
            if (type != EnemyType.NORMAL) {
                int barWidth = gp.tileSize;
                int barHeight = 4;
                int hpWidth = (int) (((double) life / maxLife) * barWidth);

                g2.setColor(Color.DARK_GRAY);
                g2.fillRect(screenX, screenY - 8, barWidth, barHeight);
                g2.setColor(Color.RED);
                g2.fillRect(screenX, screenY - 8, Math.max(0, hpWidth), barHeight);
            }
        }
    }
}