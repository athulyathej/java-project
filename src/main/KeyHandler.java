
package main;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener {

    public boolean upPressed, downPressed, leftPressed, rightPressed;
    public GamePanel gp;

    public KeyHandler(GamePanel gp) {
        this.gp = gp;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {

        int code = e.getKeyCode();

        // Normal movement during gameplay
        if (gp.gameState == GamePanel.PLAY_STATE) {

            if (code == KeyEvent.VK_W) upPressed = true;
            if (code == KeyEvent.VK_S) downPressed = true;
            if (code == KeyEvent.VK_A) leftPressed = true;
            if (code == KeyEvent.VK_D) rightPressed = true;
        }

        // Choose an upgrade during level-up
        else if (gp.gameState == GamePanel.LEVELUP_STATE) {

            if (code == KeyEvent.VK_1) {
                gp.player.applyUpgrade(1);
                gp.gameState = GamePanel.PLAY_STATE;

            } else if (code == KeyEvent.VK_2) {
                gp.player.applyUpgrade(2);
                gp.gameState = GamePanel.PLAY_STATE;

            } else if (code == KeyEvent.VK_3) {
                gp.player.applyUpgrade(3);
                gp.gameState = GamePanel.PLAY_STATE;

            } else if (code == KeyEvent.VK_4) {
                gp.player.applyUpgrade(4);
                gp.gameState = GamePanel.PLAY_STATE;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

        int code = e.getKeyCode();

        if (code == KeyEvent.VK_W) upPressed = false;
        if (code == KeyEvent.VK_S) downPressed = false;
        if (code == KeyEvent.VK_A) leftPressed = false;
        if (code == KeyEvent.VK_D) rightPressed = false;
    }
}
