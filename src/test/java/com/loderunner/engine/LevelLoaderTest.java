package com.loderunner.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loderunner.model.Level;
import java.util.List;
import org.junit.jupiter.api.Test;

class LevelLoaderTest {

    @Test
    void builtInMapsHaveExpectedSizeAndCollectibles() {
        List<Level> levels = List.of(
            LevelLoader.createDefaultLevel(),
            LevelLoader.createLevel2(),
            LevelLoader.createLevel3(),
            LevelLoader.createLevel4()
        );

        for (Level level : levels) {
            assertEquals(25, level.getWidth());
            assertEquals(16, level.getHeight());
            assertTrue(level.countGold() > 0);
        }
    }
}
