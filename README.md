# Lode Runner

[![CI](https://github.com/foudajean550-design/loderunner/actions/workflows/ci.yml/badge.svg)](https://github.com/foudajean550-design/loderunner/actions/workflows/ci.yml)

Jeu de plateforme 2D inspiré de Lode Runner, développé en **Java 21** avec **JavaFX**. Le joueur collecte des lingots, évite des adversaires pilotés par une IA et creuse des briques pour leur échapper.

## Fonctionnalités

- Quatre cartes générées et choisies au début de chaque partie.
- Déplacement, saut, échelles, cordes, collecte d’or et briques temporaires.
- Adversaires avec gravité, patrouille et poursuite du joueur.
- Mode solo et mode deux joueurs sur le même clavier.
- Prototype TCP pour l’hébergement, la connexion et les échanges de scores/actions.
- Interface dessinée avec JavaFX Canvas.

> Le mode réseau est expérimental : la synchronisation complète d’une partie entre clients reste à développer.

## Architecture

```text
com.loderunner
├── Main.java                 Démarrage JavaFX ou serveur headless
├── engine/                   Boucle de jeu, collisions et chargement des cartes
├── model/                    Joueurs, adversaires, niveaux et tuiles
├── network/                  Client TCP, serveur et gestion des connexions
└── ui/                       Menus, entrées clavier, rendu et HUD JavaFX
```

Le moteur et les modèles sont séparés de l’interface, ce qui facilite leur test et leur évolution. Les tests unitaires couvrent les règles du modèle et les cartes.

## Prérequis

- JDK 21
- Maven 3.9+

## Lancer et tester

```bash
mvn clean verify
mvn javafx:run
```

Un mode serveur headless est également disponible avec l’argument `--server`. Le mode réseau reste expérimental et ne synchronise pas encore l’état complet d’une partie.

## Commandes

| Action | Joueur 1 | Joueur 2 |
|---|---|---|
| Déplacement | Flèches ou Q/D | J/L |
| Monter / descendre | Z/S ou ↑/↓ | I/K |
| Sauter | Espace | N |
| Creuser | X | M |

Dans les menus, utilisez ↑/↓ puis Entrée. Échap revient au menu et R relance une partie terminée.
