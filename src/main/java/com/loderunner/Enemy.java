package com.loderunner.model;

/**
 * Adversaire IA : patrouille horizontale, gravité, escaliers, ensevelissement.
 *
 * Améliorations :
 *  - Vitesse limitée et constante (pas d'accélération parasite)
 *  - Montée et descente des échelles (IA monte vers le joueur)
 *  - Détection de l'état onLadder / onRope
 */
public class Enemy {

    private double  x, y;
    private double  velocityX, velocityY;
    private int     direction;   // +1 droite, -1 gauche
    private boolean buried;
    private int     buriedTimer;
    private final double spawnX, spawnY;

    // État physique
    private boolean onLadder;
    private boolean onGround;

    // IA vers le joueur
    private double  targetX, targetY;
    private int     ladderDecisionTimer = 0;

    public  static final int    BURIED_DURATION = 300;
    public  static final double SPEED           = 1.0;  // vitesse horizontale limitée
    private static final double CLIMB_SPEED     = 0.9;  // vitesse sur échelle
    private static final double GRAVITY         = 0.35;
    private static final double MAX_FALL        = 6.0;  // chute limitée

    public Enemy(double x, double y) {
        this.x = x; this.y = y;
        this.spawnX = x; this.spawnY = y;
        this.direction = 1;
        this.targetX = x;
        this.targetY = y;
    }

    public void update() {
        if (buried) {
            if (--buriedTimer <= 0) {
                x = spawnX; y = spawnY;
                buried = false;
                velocityY = 0;
                onLadder = false;
                onGround = false;
            }
            return;
        }

        if (onLadder) {
            velocityY = 0;
            ladderDecisionTimer++;
            if (ladderDecisionTimer > 8) {
                ladderDecisionTimer = 0;
                if (targetY < y - 8) {
                    velocityY = -CLIMB_SPEED;
                } else if (targetY > y + 8) {
                    velocityY = CLIMB_SPEED;
                } else {
                    velocityY = 0;
                    onLadder = false;
                }
            }
        } else {
            velocityY += GRAVITY;
            if (velocityY > MAX_FALL) velocityY = MAX_FALL;
        }

        y += velocityY;

        if (onGround && !onLadder) {
            direction = (targetX > x + 4) ? 1 : (targetX < x - 4) ? -1 : direction;
        }
        velocityX = direction * SPEED;
        if (!onLadder || Math.abs(velocityY) < 0.3) {
            x += velocityX;
        }
    }

    public void setTarget(double tx, double ty) {
        this.targetX = tx;
        this.targetY = ty;
    }

    public void reverseDirection() { direction = -direction; }

    public void bury() {
        buried = true;
        buriedTimer = BURIED_DURATION;
        velocityY = 0;
        velocityX = 0;
        onLadder = false;
    }

    public void landOn(double groundY) {
        y = groundY;
        velocityY = 0;
        onGround = true;
    }

    public void setOnLadder(boolean v) {
        this.onLadder = v;
        if (v) onGround = true;
    }

    public void setOnGround(boolean v) { this.onGround = v; }

    public double  getX()           { return x; }
    public double  getY()           { return y; }
    public void    setX(double v)   { x = v; }
    public void    setY(double v)   { y = v; }
    public double  getVelocityY()   { return velocityY; }
    public double  getVelocityX()   { return velocityX; }
    public boolean isBuried()       { return buried; }
    public boolean isOnLadder()     { return onLadder; }
    public boolean isOnGround()     { return onGround; }
    public int     getDirection()   { return direction; }
}
