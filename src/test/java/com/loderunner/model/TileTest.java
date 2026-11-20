package com.loderunner.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TileTest {

    @Test
    void dugBrickTemporarilyBecomesPassableAndThenRebuilds() {
        Tile tile = new Tile(TileType.DIG);

        tile.dig();
        assertTrue(tile.isDug());
        assertFalse(tile.isSolid());

        for (int i = 0; i < Tile.DIG_DURATION - 1; i++) {
            tile.update();
        }
        assertTrue(tile.isDug());

        tile.update();
        assertFalse(tile.isDug());
        assertTrue(tile.isSolid());
    }

    @Test
    void solidTileCannotBeDug() {
        Tile tile = new Tile(TileType.SOLID);

        tile.dig();

        assertFalse(tile.isDug());
        assertTrue(tile.isSolid());
    }
}
