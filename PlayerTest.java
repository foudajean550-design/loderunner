package com.loderunner.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlayerTest {

    @Test
    void jumpStartsOnlyWhenGrounded() {
        Player player = new Player(10, 20);

        player.jump();
        assertEquals(0, player.getVelocityY());

        player.setOnGround(true);
        player.jump();

        assertEquals(Player.JUMP_FORCE, player.getVelocityY());
        assertFalse(player.isOnGround());
        player.update();
        assertTrue(player.getY() < 20);
    }
}
