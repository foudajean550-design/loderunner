package com.loderunner.model;

/**
 * Grille 2D de tuiles représentant un niveau.
 * Accès sécurisé : hors bornes → tuile SOLID fictive.
 */
public class Level {

    private final Tile[][] tiles;
    private final int      width;
    private final int      height;
    private int            totalGold;

    public Level(int width, int height) {
        this.width  = width;
        this.height = height;
        tiles = new Tile[width][height];
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                tiles[x][y] = new Tile(TileType.EMPTY);
    }

    /** Retourne SOLID pour toute position hors limites (évite les NPE). */
    public Tile getTile(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height)
            return new Tile(TileType.SOLID);
        return tiles[x][y];
    }

    public void setTile(int x, int y, Tile t) {
        if (x >= 0 && x < width && y >= 0 && y < height)
            tiles[x][y] = t;
    }

    /** Met à jour la régénération de toutes les briques creusées. */
    public void update() {
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                tiles[x][y].update();
    }

    /** Nombre de lingots restants (condition de victoire). */
    public int countGold() {
        int n = 0;
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                if (tiles[x][y].isCollectible()) n++;
        return n;
    }

    public int  getWidth()          { return width; }
    public int  getHeight()         { return height; }
    public int  getTotalGold()      { return totalGold; }
    public void setTotalGold(int g) { totalGold = g; }
}
