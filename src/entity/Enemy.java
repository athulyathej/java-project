package entity;

import main.GamePanel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Enemy extends Entity {

    GamePanel gp;

    public Enemy(GamePanel gp, int worldX, int worldY) {
        this.maxLife = 3;
        this.life = this.maxLife;
        this.gp = gp;
        this.worldX = worldX;
        this.worldY = worldY;
        this.speed = 2; // Slower than player
        this.solidArea = new Rectangle(12, 16, 24, 24); // Hitbox
        this.direction = "down";
        this.alive= true;
    }

    public void update() {
        int xDiff = gp.player.worldX - worldX;
        int yDiff = gp.player.worldY - worldY;

        // Determine primary and secondary axis for movement
        String primary = "", secondary = "";
        if (Math.abs(xDiff) > Math.abs(yDiff)) {
            primary = (xDiff > 0) ? "right" : "left";
            secondary = (yDiff > 0) ? "down" : "up";
        } else {
            primary = (yDiff > 0) ? "down" : "up";
            secondary = (xDiff > 0) ? "right" : "left";
        }

        // Try primary direction
        direction = primary;
        collisionOn = false;
        gp.cChecker.checkTile(this);

        // If blocked, try secondary direction
        if (collisionOn) {
            direction = secondary;
            collisionOn = false;
            gp.cChecker.checkTile(this);
        }

        // Move if no collision
        if (!collisionOn) {
            switch(direction) {
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
            System.out.println("Player Killed!");
            System.exit(0); // Instantly closes the game.
        }
    }

    public void draw(Graphics2D g2) {
        int screenX = worldX - gp.player.worldX + gp.player.screenX;
        int screenY = worldY - gp.player.worldY + gp.player.screenY;

        if(worldX + gp.tileSize > gp.player.worldX - gp.player.screenX &&
                worldX - gp.tileSize < gp.player.worldX + gp.player.screenX &&
                worldY + gp.tileSize > gp.player.worldY - gp.player.screenY &&
                worldY - gp.tileSize < gp.player.worldY + gp.player.screenY) {

            g2.setColor(Color.RED);
            g2.fillRect(screenX, screenY, gp.tileSize, gp.tileSize);
        }
    }
}