package entity;

import main.GamePanel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Projectile extends Entity {
    GamePanel gp;
    Enemy target;
    public int damage = 1;

    public Projectile(GamePanel gp) {
        this.gp = gp;
        this.speed = 6;
        this.solidArea = new Rectangle(0, 0, 8, 8);
    }

    public void spawn(int startX, int startY, Enemy target, int damage) {
        this.worldX = startX;
        this.worldY = startY;
        this.target = target;
        this.damage = damage;
        this.alive = true;
    }

    public void update() {
        if (target == null || !target.alive) {
            alive = false;
            return;
        }

        int xDiff = target.worldX - worldX;
        int yDiff = target.worldY - worldY;
        double distance = Math.sqrt(xDiff * xDiff + yDiff * yDiff);

        if (distance < speed) {
            target.life -= damage;
            if (target.life <= 0) target.alive = false;
            alive = false;
        } else {
            worldX += (int) ((xDiff / distance) * speed);
            worldY += (int) ((yDiff / distance) * speed);
        }
    }

    public void draw(Graphics2D g2) {
        int screenX = worldX - gp.player.worldX + gp.player.screenX;
        int screenY = worldY - gp.player.worldY + gp.player.screenY;

        g2.setColor(Color.YELLOW);
        g2.fillOval(screenX, screenY, solidArea.width, solidArea.height);
    }
}