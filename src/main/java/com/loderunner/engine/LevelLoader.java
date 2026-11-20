package com.loderunner.engine;

import com.loderunner.model.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Random;

/**
 * Charge un niveau depuis un fichier texte ASCII ou génère un niveau aléatoire
 * parmi 4 niveaux disponibles.
 *
 * Correspondance des caractères :
 *   #  SOLID       B  DIG       H  LADDER
 *   -  ROPE        G  COLLECTIBLE   .  EMPTY
 */
public class LevelLoader {

    private static final Random RNG = new Random();

    /** Charge un niveau aléatoire parmi les 4 disponibles. */
    public static Level loadRandom() {
        int idx = RNG.nextInt(4);
        return switch (idx) {
            case 0  -> createDefaultLevel();
            case 1  -> createLevel2();
            case 2  -> createLevel3();
            default -> createLevel4();
        };
    }

    public static Level loadFromFile(String path) {
        try {
            List<String> lines = Files.readAllLines(Path.of(path));
            if (lines.isEmpty()) return loadRandom();
            int w = lines.get(0).length(), h = lines.size();
            Level lvl = new Level(w, h);
            int gold = 0;
            for (int y = 0; y < h; y++) {
                String line = lines.get(y);
                for (int x = 0; x < Math.min(line.length(), w); x++) {
                    TileType t = parse(line.charAt(x));
                    lvl.setTile(x, y, new Tile(t));
                    if (t == TileType.COLLECTIBLE) gold++;
                }
            }
            lvl.setTotalGold(gold);
            return lvl;
        } catch (IOException e) {
            System.err.println("Niveau introuvable, niveau aléatoire chargé.");
            return loadRandom();
        }
    }

    private static TileType parse(char c) {
        return switch (c) {
            case '#' -> TileType.SOLID;
            case 'B' -> TileType.DIG;
            case 'H' -> TileType.LADDER;
            case '-' -> TileType.ROPE;
            case 'G' -> TileType.COLLECTIBLE;
            default  -> TileType.EMPTY;
        };
    }

    // ─────────────────────────── NIVEAU 1 ──────────────────────────────────
    /** Niveau classique avec plusieurs plateformes et échelles. */
    public static Level createDefaultLevel() {
        int W = 25, H = 16;
        Level lvl = new Level(W, H);

        // Sol et murs
        for (int x = 0; x < W; x++) lvl.setTile(x, H-1, new Tile(TileType.SOLID));
        for (int y = 0; y < H; y++) {
            lvl.setTile(0,   y, new Tile(TileType.SOLID));
            lvl.setTile(W-1, y, new Tile(TileType.SOLID));
        }

        // Plateformes solides
        for (int x = 1; x <  9; x++) lvl.setTile(x, 12, new Tile(TileType.SOLID));
        for (int x = 9; x < 17; x++) lvl.setTile(x,  9, new Tile(TileType.SOLID));
        for (int x = 1; x < 13; x++) lvl.setTile(x,  6, new Tile(TileType.SOLID));
        for (int x =14; x < 24; x++) lvl.setTile(x,  6, new Tile(TileType.SOLID));
        for (int x =16; x < 24; x++) lvl.setTile(x, 12, new Tile(TileType.SOLID));

        // Briques creusables
        for (int x = 2; x <  7; x++) lvl.setTile(x, 12, new Tile(TileType.DIG));
        for (int x =10; x < 15; x++) lvl.setTile(x,  9, new Tile(TileType.DIG));
        for (int x =17; x < 22; x++) lvl.setTile(x, 12, new Tile(TileType.DIG));

        // Échelles
        for (int y = 10; y <= 12; y++) lvl.setTile(8,  y, new Tile(TileType.LADDER));
        for (int y =  7; y <=  9; y++) lvl.setTile(13, y, new Tile(TileType.LADDER));
        for (int y =  7; y <= 12; y++) lvl.setTile(15, y, new Tile(TileType.LADDER));
        for (int y =  4; y <=  6; y++) lvl.setTile(6,  y, new Tile(TileType.LADDER));

        // Cordes
        for (int x = 3; x < 12; x++) lvl.setTile(x, 3, new Tile(TileType.ROPE));
        for (int x =14; x < 22; x++) lvl.setTile(x, 3, new Tile(TileType.ROPE));

        // Lingots d'or
        int[] gx = {3, 5, 7,  11, 13, 16, 18, 20, 4, 10, 17};
        int[] gy = {11,11,11,  8,  8,  11, 11, 11, 5,  5,  5};
        int gold = 0;
        for (int i = 0; i < gx.length; i++) {
            lvl.setTile(gx[i], gy[i], new Tile(TileType.COLLECTIBLE));
            gold++;
        }
        lvl.setTotalGold(gold);
        return lvl;
    }

    // ─────────────────────────── NIVEAU 2 ──────────────────────────────────
    /** Tour : niveaux superposés reliés par des échelles centrales. */
    public static Level createLevel2() {
        int W = 25, H = 16;
        Level lvl = new Level(W, H);

        // Murs et sol
        for (int x = 0; x < W; x++) lvl.setTile(x, H-1, new Tile(TileType.SOLID));
        for (int y = 0; y < H; y++) {
            lvl.setTile(0,   y, new Tile(TileType.SOLID));
            lvl.setTile(W-1, y, new Tile(TileType.SOLID));
        }

        // Étages horizontaux
        int[] floors = {13, 10, 7, 4};
        for (int floor : floors) {
            for (int x = 1; x < W-1; x++)
                lvl.setTile(x, floor, new Tile(TileType.SOLID));
        }

        // Ouvertures dans les étages (passages)
        int[] openCols = {5, 12, 19};
        for (int floor : floors) {
            for (int col : openCols) {
                lvl.setTile(col,   floor, new Tile(TileType.EMPTY));
                lvl.setTile(col+1, floor, new Tile(TileType.EMPTY));
            }
        }

        // Échelles aux ouvertures
        for (int col : openCols) {
            for (int y = 4; y < H-1; y++)
                lvl.setTile(col, y, new Tile(TileType.LADDER));
        }

        // Briques creusables sur chaque étage
        int[] digCols = {3, 8, 15, 22};
        for (int floor : floors) {
            for (int dc : digCols)
                if (dc < W-1) lvl.setTile(dc, floor, new Tile(TileType.DIG));
        }

        // Cordes
        for (int x = 2; x < W-2; x++) lvl.setTile(x, 2, new Tile(TileType.ROPE));

        // Or sur chaque étage
        int gold = 0;
        int[][] goldPos = {
            {3,12},{7,12},{10,12},{14,12},{17,12},{21,12},
            {3, 9},{7, 9},{10, 9},{14, 9},{17, 9},{21, 9},
            {3, 6},{7, 6},{14, 6},{21, 6},
            {3, 3},{12,3},{21,3}
        };
        for (int[] g : goldPos) {
            if (lvl.getTile(g[0],g[1]).getType() == TileType.EMPTY) {
                lvl.setTile(g[0], g[1], new Tile(TileType.COLLECTIBLE));
                gold++;
            }
        }
        lvl.setTotalGold(gold);
        return lvl;
    }

    // ─────────────────────────── NIVEAU 3 ──────────────────────────────────
    /** Labyrinthe en zigzag avec cordes et échelles alternées. */
    public static Level createLevel3() {
        int W = 25, H = 16;
        Level lvl = new Level(W, H);

        // Murs et sol
        for (int x = 0; x < W; x++) lvl.setTile(x, H-1, new Tile(TileType.SOLID));
        for (int y = 0; y < H; y++) {
            lvl.setTile(0,   y, new Tile(TileType.SOLID));
            lvl.setTile(W-1, y, new Tile(TileType.SOLID));
        }

        // Plateformes en zigzag (gauche ↔ droite alternées)
        // Rang 1 : gauche
        for (int x = 1; x < 16; x++) lvl.setTile(x, 13, new Tile(TileType.SOLID));
        // Rang 2 : droite
        for (int x = 9; x < W-1; x++) lvl.setTile(x, 10, new Tile(TileType.SOLID));
        // Rang 3 : gauche
        for (int x = 1; x < 18; x++) lvl.setTile(x, 7, new Tile(TileType.SOLID));
        // Rang 4 : droite (haute)
        for (int x = 7; x < W-1; x++) lvl.setTile(x, 4, new Tile(TileType.SOLID));

        // Briques creusables
        for (int x = 2; x <  5; x++) lvl.setTile(x, 13, new Tile(TileType.DIG));
        for (int x =10; x < 13; x++) lvl.setTile(x, 10, new Tile(TileType.DIG));
        for (int x = 3; x <  6; x++) lvl.setTile(x,  7, new Tile(TileType.DIG));
        for (int x =16; x < 19; x++) lvl.setTile(x,  4, new Tile(TileType.DIG));

        // Échelles
        for (int y = 11; y <= 13; y++) lvl.setTile(15, y, new Tile(TileType.LADDER));
        for (int y =  8; y <= 10; y++) lvl.setTile(9,  y, new Tile(TileType.LADDER));
        for (int y =  5; y <=  7; y++) lvl.setTile(17, y, new Tile(TileType.LADDER));
        for (int y =  2; y <=  4; y++) lvl.setTile(7,  y, new Tile(TileType.LADDER));
        // Échelle de connexion sol → rang 1
        for (int y = 14; y < H-1; y++) lvl.setTile(4, y, new Tile(TileType.LADDER));

        // Cordes
        for (int x = 2; x < 22; x++) lvl.setTile(x, 2, new Tile(TileType.ROPE));

        // Or
        int gold = 0;
        int[][] goldPos = {
            {3,12},{6,12},{9,12},{12,12},
            {10,9},{13,9},{16,9},{20,9},
            {2,6},{5,6},{8,6},{11,6},{14,6},
            {8,3},{11,3},{15,3},{19,3},{22,3}
        };
        for (int[] g : goldPos) {
            if (g[0]>0 && g[0]<W-1 && g[1]>0 && g[1]<H-1 &&
                lvl.getTile(g[0],g[1]).getType() == TileType.EMPTY) {
                lvl.setTile(g[0], g[1], new Tile(TileType.COLLECTIBLE));
                gold++;
            }
        }
        lvl.setTotalGold(gold);
        return lvl;
    }

    // ─────────────────────────── NIVEAU 4 ──────────────────────────────────
    /** Pyramide inversée : large en bas, étroit en haut. */
    public static Level createLevel4() {
        int W = 25, H = 16;
        Level lvl = new Level(W, H);

        // Murs et sol
        for (int x = 0; x < W; x++) lvl.setTile(x, H-1, new Tile(TileType.SOLID));
        for (int y = 0; y < H; y++) {
            lvl.setTile(0,   y, new Tile(TileType.SOLID));
            lvl.setTile(W-1, y, new Tile(TileType.SOLID));
        }

        // Plateforme basse large
        for (int x = 1; x < W-1; x++) lvl.setTile(x, 12, new Tile(TileType.SOLID));
        // Plateforme intermédiaire
        for (int x = 4; x < W-4; x++) lvl.setTile(x, 9, new Tile(TileType.SOLID));
        // Plateforme intermédiaire haute
        for (int x = 7; x < W-7; x++) lvl.setTile(x, 6, new Tile(TileType.SOLID));
        // Plateforme sommet
        for (int x = 10; x < W-10; x++) lvl.setTile(x, 3, new Tile(TileType.SOLID));

        // Briques creusables
        for (int x = 2; x <  6; x++) lvl.setTile(x, 12, new Tile(TileType.DIG));
        for (int x =19; x < 23; x++) lvl.setTile(x, 12, new Tile(TileType.DIG));
        for (int x = 5; x <  8; x++) lvl.setTile(x,  9, new Tile(TileType.DIG));
        for (int x =17; x < 20; x++) lvl.setTile(x,  9, new Tile(TileType.DIG));
        for (int x = 8; x < 10; x++) lvl.setTile(x,  6, new Tile(TileType.DIG));
        for (int x =15; x < 17; x++) lvl.setTile(x,  6, new Tile(TileType.DIG));

        // Échelles reliant chaque étage (symétrique)
        for (int y = 10; y <= 12; y++) lvl.setTile(4,  y, new Tile(TileType.LADDER));
        for (int y = 10; y <= 12; y++) lvl.setTile(20, y, new Tile(TileType.LADDER));
        for (int y =  7; y <=  9; y++) lvl.setTile(7,  y, new Tile(TileType.LADDER));
        for (int y =  7; y <=  9; y++) lvl.setTile(17, y, new Tile(TileType.LADDER));
        for (int y =  4; y <=  6; y++) lvl.setTile(10, y, new Tile(TileType.LADDER));
        for (int y =  4; y <=  6; y++) lvl.setTile(14, y, new Tile(TileType.LADDER));

        // Cordes
        for (int x = 2; x < W-2; x++) lvl.setTile(x, 1, new Tile(TileType.ROPE));

        // Or réparti sur les étages
        int gold = 0;
        int[][] goldPos = {
            {3,11},{7,11},{10,11},{12,11},{15,11},{18,11},{22,11},
            {5,8},{8,8},{12,8},{16,8},{19,8},
            {8,5},{10,5},{12,5},{14,5},{16,5},
            {11,2},{12,2},{13,2}
        };
        for (int[] g : goldPos) {
            if (g[0]>0 && g[0]<W-1 && g[1]>0 && g[1]<H-1 &&
                lvl.getTile(g[0],g[1]).getType() == TileType.EMPTY) {
                lvl.setTile(g[0], g[1], new Tile(TileType.COLLECTIBLE));
                gold++;
            }
        }
        lvl.setTotalGold(gold);
        return lvl;
    }
}
