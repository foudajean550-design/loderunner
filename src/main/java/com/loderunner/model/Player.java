package com.loderunner.model;

/**
 * Joueur : position, physique, score, vies, direction de regard.
 *
 * Le joueur peut se déplacer, sauter et monter ou descendre les échelles.
 */
public class Player {

    private double  x, y;
    private double  velocityX, velocityY;
    private boolean onGround;
    private boolean onLadder;
    private boolean onRope;
    private int     score;
    private int     lives;
    private boolean facingRight;

    // --- Constantes physiques ---
    public  static final double SPEED        = 2.0;
    public  static final double GRAVITY      = 0.4;
    public  static final double JUMP_FORCE   = -7.0;
    public  static final double CLIMB_SPEED  = 1.8;
    public  static final double MAX_FALL     = 7.0;
    public  static final int    MAX_LIVES    = 5;

    public Player(double x, double y) {
        this.x           = x;
        this.y           = y;
        this.score       = 0;
        this.lives       = MAX_LIVES;
        this.facingRight = true;
    }

    public void update() {
        if (!onLadder && !onRope) {
            velocityY += GRAVITY;
            if (velocityY > MAX_FALL) velocityY = MAX_FALL;
        }
        // Limiter la vitesse horizontale
        if (velocityX >  SPEED) velocityX =  SPEED;
        if (velocityX < -SPEED) velocityX = -SPEED;

        x += velocityX;
        y += velocityY;
    }

    public void moveLeft()  { velocityX = -SPEED; facingRight = false; }
    public void moveRight() { velocityX =  SPEED; facingRight = true;  }
    public void stopX()     { velocityX = 0; }

    public void climbUp() {
        if (onLadder) { velocityY = -CLIMB_SPEED; }
    }
    public void climbDown() {
        if (onLadder || onRope) { velocityY = CLIMB_SPEED; }
    }
    public void stopY() { velocityY = 0; }

    public void jump() {
        if (onGround && !onRope) {
            velocityY = JUMP_FORCE;
            onGround  = false;
            onLadder  = false;
        }
    }

    public void setOnGround(boolean v) {
        onGround = v;
        if (v) velocityY = 0;
    }

    public void addScore(int s)   { score += s; }
    public void loseLife()        { if (lives > 0) lives--; }
    public boolean isAlive()      { return lives > 0; }

    // --- Getters / Setters ---
    public double  getX()               { return x; }
    public double  getY()               { return y; }
    public void    setX(double x)       { this.x = x; }
    public void    setY(double y)       { this.y = y; }
    public double  getVelocityX()       { return velocityX; }
    public double  getVelocityY()       { return velocityY; }
    public void    setVelocityY(double v){ this.velocityY = v; }
    public boolean isOnGround()         { return onGround; }
    public boolean isOnLadder()         { return onLadder; }
    public boolean isOnRope()           { return onRope; }
    public void    setOnLadder(boolean v){ this.onLadder = v; }
    public void    setOnRope(boolean v)  { this.onRope = v; }
    public boolean isFacingRight()      { return facingRight; }
    public int     getScore()           { return score; }
    public int     getLives()           { return lives; }
    public void    setLives(int l)      { this.lives = l; }
    public void    setScore(int s)      { this.score = s; }
}
