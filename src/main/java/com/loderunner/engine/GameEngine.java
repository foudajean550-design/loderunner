package com.loderunner.engine;

import com.loderunner.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Moteur de jeu : physique, collisions, score, vies, victoire/défaite.
 *
 * Nouveautés :
 *  - Mode 2 joueurs locaux (player1 + player2)
 *  - Les IA montent/descendent les échelles et suivent le joueur le plus proche
 *  - Vitesse des IA et joueurs limitée
 *  - Niveau chargé aléatoirement parmi 4 maps
 */
public class GameEngine {

    private Player      player;    // Joueur 1
    private Player      player2;   // Joueur 2 (mode multijoueur)
    private Level       level;
    private List<Enemy> enemies;

    public static final int TILE_SIZE   = 32;
    public static final int ENTITY_SIZE = 28;

    private boolean gameOver;
    private boolean victory;
    private boolean twoPlayerMode;
    private String  currentMapName = "Map 1";

    // Positions de départ
    private static final double START_X1 = 32 + 4;
    private static final double START_Y1 = 10 * 32;
    private static final double START_X2 = 22 * 32 + 4;
    private static final double START_Y2 = 10 * 32;

    public GameEngine() {
        reset(false);
    }

    /** Réinitialise la partie. */
    public void reset(boolean twoPlayers) {
        this.twoPlayerMode = twoPlayers;
        int mapIdx = (int)(Math.random() * 4);
        level = switch (mapIdx) {
            case 0  -> { currentMapName = "Map 1 — Classique";    yield LevelLoader.createDefaultLevel(); }
            case 1  -> { currentMapName = "Map 2 — La Tour";      yield LevelLoader.createLevel2(); }
            case 2  -> { currentMapName = "Map 3 — Zigzag";       yield LevelLoader.createLevel3(); }
            default -> { currentMapName = "Map 4 — Pyramide";     yield LevelLoader.createLevel4(); }
        };

        player   = new Player(START_X1, START_Y1);
        player2  = new Player(START_X2, START_Y2);

        enemies  = new ArrayList<>();
        enemies.add(new Enemy(10 * TILE_SIZE, 8 * TILE_SIZE));
        enemies.add(new Enemy(18 * TILE_SIZE, 5 * TILE_SIZE));
        enemies.add(new Enemy(20 * TILE_SIZE, 11 * TILE_SIZE));
        gameOver = false;
        victory  = false;
    }

    // Compatibilité avec l'ancienne signature (mode solo par défaut)
    public void reset() { reset(false); }

    /** Mise à jour d'une frame. */
    public void update() {
        if (gameOver) return;

        player.update();
        resolvePlayerCollisions(player);

        if (twoPlayerMode) {
            player2.update();
            resolvePlayerCollisions(player2);
        }

        level.update();

        for (Enemy e : enemies) {
            // Trouver le joueur le plus proche comme cible
            double tx, ty;
            if (twoPlayerMode && player2.isAlive()) {
                double d1 = dist(e, player);
                double d2 = dist(e, player2);
                if (d1 < d2) { tx = player.getX();  ty = player.getY(); }
                else          { tx = player2.getX(); ty = player2.getY(); }
            } else {
                tx = player.getX();
                ty = player.getY();
            }
            e.setTarget(tx, ty);
            e.update();
            if (!e.isBuried()) resolveEnemyCollisions(e);
        }

        checkPlayerEnemyCollisions(player, false);
        if (twoPlayerMode) checkPlayerEnemyCollisions(player2, true);

        // Collecte d'or pour joueur 2
        if (twoPlayerMode) collectGold(player2);

        if (level.countGold() == 0) { victory = true; gameOver = true; }
        if (!player.isAlive() && (!twoPlayerMode || !player2.isAlive())) {
            gameOver = true;
        }
    }

    private double dist(Enemy e, Player p) {
        double dx = e.getX() - p.getX();
        double dy = e.getY() - p.getY();
        return Math.sqrt(dx*dx + dy*dy);
    }

    // ── Collisions joueur ──────────────────────────────────────────────────

    private void resolvePlayerCollisions(Player p) {
        int T = TILE_SIZE, S = ENTITY_SIZE;

        // 1. Collision verticale
        int left  = (int)(p.getX() / T);
        int right = (int)((p.getX() + S - 1) / T);

        int bottomTile = (int)((p.getY() + S) / T);
        if (level.getTile(left, bottomTile).isSolid() ||
            level.getTile(right, bottomTile).isSolid()) {
            p.setOnGround(true);
            p.setY(bottomTile * T - S);
        } else {
            p.setOnGround(false);
        }

        if (p.getVelocityY() < 0) {
            int topTile = (int)(p.getY() / T);
            if (level.getTile(left, topTile).isSolid() ||
                level.getTile(right, topTile).isSolid()) {
                p.setVelocityY(0);
                p.setY((topTile + 1) * T);
            }
        }

        // 2. Collision horizontale
        int top    = (int)(p.getY() / T);
        int bottom = (int)((p.getY() + S - 1) / T);

        if (p.getVelocityX() > 0) {
            int rightTile = (int)((p.getX() + S) / T);
            if (level.getTile(rightTile, top).isSolid() ||
                level.getTile(rightTile, bottom).isSolid()) {
                p.setX(rightTile * T - S);
            }
        } else if (p.getVelocityX() < 0) {
            int leftTile = (int)(p.getX() / T);
            if (level.getTile(leftTile, top).isSolid() ||
                level.getTile(leftTile, bottom).isSolid()) {
                p.setX((leftTile + 1) * T);
            }
        }

        // 3. Échelle et corde
        int centerX = (int)((p.getX() + S / 2.0) / T);
        int centerY = (int)((p.getY() + S / 2.0) / T);
        Tile center = level.getTile(centerX, centerY);

        boolean wasOnLadder = p.isOnLadder();
        p.setOnLadder(center.isLadder());
        p.setOnRope(center.isRope());

        if (p.isOnLadder()) {
            p.setOnGround(true);
            if (!wasOnLadder) p.setVelocityY(0);
        }
        if (p.isOnRope()) {
            p.setVelocityY(0);
            p.setY(centerY * T + 2);
        }

        // 4. Collecte d'or
        collectGold(p);

        // 5. Bornes
        double maxY = (level.getHeight() - 1) * T - S;
        double maxX = (level.getWidth()  - 1) * T - S;
        if (p.getY() > maxY) { p.setY(maxY); p.setOnGround(true); }
        if (p.getX() < T)    p.setX(T);
        if (p.getX() > maxX) p.setX(maxX);
    }

    private void collectGold(Player p) {
        int T = TILE_SIZE, S = ENTITY_SIZE;
        int cx = (int)((p.getX() + S / 2.0) / T);
        int cy = (int)((p.getY() + S / 2.0) / T);
        Tile t = level.getTile(cx, cy);
        if (t.isCollectible()) {
            p.addScore(10);
            level.setTile(cx, cy, new Tile(TileType.EMPTY));
        }
    }

    /** Creusage : appelé depuis l'UI. */
    public void playerDig(Player p) {
        if (!p.isOnGround()) return;
        int T = TILE_SIZE, S = ENTITY_SIZE;
        int py = (int)((p.getY() + S) / T);
        int digX = p.isFacingRight()
                   ? (int)((p.getX() + S) / T)
                   : (int)((p.getX() - 1) / T);
        level.getTile(digX, py - 1).dig();
    }

    // Compatibilité solo
    public void playerDig() { playerDig(player); }

    // ── Collisions IA ──────────────────────────────────────────────────────

    private void resolveEnemyCollisions(Enemy e) {
        int T = TILE_SIZE, S = ENTITY_SIZE;
        int ex = (int)(e.getX() / T);
        int ey = (int)((e.getY() + S) / T);

        // Sol
        if (level.getTile(ex, ey).isSolid() ||
            level.getTile((int)((e.getX() + S - 1) / T), ey).isSolid()) {
            e.landOn(ey * T - S);
        }

        // Mur → inversion
        int nextX  = (int)(e.getX() / T) + e.getDirection();
        int eyTop  = (int)(e.getY() / T);
        if (level.getTile(nextX, eyTop).isSolid() ||
            level.getTile(nextX, (int)((e.getY() + S - 1) / T)).isSolid()) {
            e.reverseDirection();
        }

        // Échelle : l'IA la détecte et monte si elle est dessus
        int centerX = (int)((e.getX() + S / 2.0) / T);
        int centerY = (int)((e.getY() + S / 2.0) / T);
        Tile centerTile = level.getTile(centerX, centerY);
        e.setOnLadder(centerTile.isLadder());

        // Ensevelissement
        int eyMid = (int)((e.getY() + S / 2.0) / T);
        if (level.getTile(ex, eyMid + 1).isDug()) {
            e.bury();
        }

        // Bornes
        double maxX = (level.getWidth()  - 1) * T - S;
        if (e.getX() < T)    { e.setX(T);    e.reverseDirection(); }
        if (e.getX() > maxX) { e.setX(maxX); e.reverseDirection(); }
    }

    // ── Collision joueur/ennemi ────────────────────────────────────────────

    private void checkPlayerEnemyCollisions(Player p, boolean isP2) {
        if (!p.isAlive()) return;
        double px = p.getX(), py = p.getY();
        int S = ENTITY_SIZE;
        for (Enemy e : enemies) {
            if (e.isBuried()) continue;
            if (px < e.getX() + S && px + S > e.getX() &&
                py < e.getY() + S && py + S > e.getY()) {
                p.loseLife();
                if (isP2) respawn(p, START_X2, START_Y2);
                else       respawn(p, START_X1, START_Y1);
            }
        }
    }

    private void respawn(Player p, double sx, double sy) {
        p.setX(sx); p.setY(sy);
        p.setVelocityY(0);
        p.setOnGround(false);
        p.setOnLadder(false);
        p.setOnRope(false);
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public Player      getPlayer()       { return player; }
    public Player      getPlayer2()      { return player2; }
    public Level       getLevel()        { return level; }
    public List<Enemy> getEnemies()      { return enemies; }
    public boolean     isGameOver()      { return gameOver; }
    public boolean     isVictory()       { return victory; }
    public boolean     isTwoPlayerMode() { return twoPlayerMode; }
    public String      getMapName()      { return currentMapName; }
}
