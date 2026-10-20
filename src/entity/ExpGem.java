package entity;

import main.GamePanel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class ExpGem extends Entity {
    GamePanel gp;
    public int expValue = 10;

    public ExpGem(GamePanel gp) {
        this.gp = gp;
        this.solidArea = new Rectangle(0, 0, 12, 12);
    }

    public void spawn(int worldX, int worldY, int expValue) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.expValue = expValue;
        this.alive = true;
    }

    public void draw(Graphics2D g2) {
        int screenX = worldX - gp.player.worldX + gp.player.screenX;
        int screenY = worldY - gp.player.worldY + gp.player.screenY;

        g2.setColor(Color.BLUE);
        g2.fillRect(screenX, screenY, solidArea.width, solidArea.height);
    }
}