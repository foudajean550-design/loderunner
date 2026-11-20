package com.loderunner.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LevelTest {

    @Test
    void countsCollectiblesAndTreatsOutOfBoundsAsSolid() {
        Level level = new Level(3, 2);
        level.setTile(1, 1, new Tile(TileType.COLLECTIBLE));

        assertEquals(1, level.countGold());
        assertTrue(level.getTile(-1, 0).isSolid());
        assertTrue(level.getTile(3, 1).isSolid());
    }
}
