package com.loderunner.model;

/**
 * Case du niveau : type, état de creusage, timer de régénération.
 */
public class Tile {

    private TileType type;
    private boolean  dug;
    private int      digTimer;

    /** 3 secondes à 60 fps avant régénération d'une brique creusée. */
    public static final int DIG_DURATION = 180;

    public Tile(TileType type) {
        this.type     = type;
        this.dug      = false;
        this.digTimer = 0;
    }

    /** Décrément du timer ; la brique se rebouche quand il atteint 0. */
    public void update() {
        if (dug && digTimer > 0) {
            digTimer--;
            if (digTimer == 0) {
                dug  = false;
                type = TileType.DIG; // redevient creusable
            }
        }
    }

    /** Creuse la brique si possible. */
    public void dig() {
        if (type == TileType.DIG && !dug) {
            dug      = true;
            digTimer = DIG_DURATION;
        }
    }

    /** Bloque-t-il le passage ? Une brique creusée ne bloque plus. */
    public boolean isSolid() {
        return (type == TileType.SOLID || type == TileType.DIG) && !dug;
    }

    public boolean isCollectible() { return type == TileType.COLLECTIBLE; }
    public boolean isLadder()      { return type == TileType.LADDER; }
    public boolean isRope()        { return type == TileType.ROPE; }
    public boolean isDug()         { return dug; }
    public TileType getType()      { return type; }
    public void setType(TileType t){ this.type = t; }
}
