package entity;

import main.GamePanel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class ExpGem extends Entity {

    GamePanel gp;

    public int expValue = 10;

    public ExpGem(GamePanel gp, int worldX, int worldY) {

        this.gp = gp;

        this.worldX = worldX;
        this.worldY = worldY;

        this.alive = true;

        this.solidArea = new Rectangle(0, 0, 12, 12);
    }

    public void draw(Graphics2D g2) {

        int screenX = worldX - gp.player.worldX + gp.player.screenX;
        int screenY = worldY - gp.player.worldY + gp.player.screenY;

        g2.setColor(Color.BLUE);

        g2.fillRect(
                screenX,
                screenY,
                solidArea.width,
                solidArea.height
        );
    }
}