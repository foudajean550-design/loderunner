package com.loderunner.model;

/**
 * Types de cases d'un niveau Lode Runner.
 * Chaque valeur détermine le comportement physique de la tuile.
 */
public enum TileType {
    EMPTY,        // vide, gravité active
    SOLID,        // mur infranchissable
    COLLECTIBLE,  // lingot d'or (+10 pts)
    LADDER,       // échelle : annule la gravité, montée/descente
    ROPE,         // corde : déplacement horizontal suspendu
    DIG           // brique creusable temporairement
}
