package entity;

import main.GamePanel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Projectile extends Entity {
    GamePanel gp;
    Enemy target;

    public Projectile(GamePanel gp, int startX, int startY, Enemy target) {
        this.gp = gp;
        this.worldX = startX;
        this.worldY = startY;
        this.target = target;
        this.speed = 6;
        this.solidArea = new Rectangle(0, 0, 8, 8);
    }

    public void update() {
        if (!target.alive) {
            alive = false;
            return;
        }

        // Calculate trajectory towards target
        int xDiff = target.worldX - worldX;
        int yDiff = target.worldY - worldY;
        double distance = Math.sqrt(xDiff * xDiff + yDiff * yDiff);

        // Collision and damage logic
        if (distance < speed) {
            target.life -= 1;
            if (target.life <= 0) target.alive = false;
            alive = false;
        } else {
            worldX += (int)((xDiff / distance) * speed);
            worldY += (int)((yDiff / distance) * speed);
        }
    }

    public void draw(Graphics2D g2) {
        int screenX = worldX - gp.player.worldX + gp.player.screenX;
        int screenY = worldY - gp.player.worldY + gp.player.screenY;

        g2.setColor(Color.YELLOW);
        g2.fillOval(screenX, screenY, solidArea.width, solidArea.height);
    }
}
