package com.loderunner.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import com.loderunner.engine.GameEngine;
import com.loderunner.model.*;
import com.loderunner.network.GameClient;
import com.loderunner.network.GameServer;

import java.util.HashSet;
import java.util.Set;

/**
 * Couche UI JavaFX : menu principal, jeu solo, jeu 2 joueurs, overlay fin.
 *
 * États :
 *  MENU      → menu principal
 *  PLAYING   → partie en cours
 *  GAME_OVER → overlay victoire/défaite
 *
 * Contrôles Joueur 1 :  ←/→ ou Q/D | ↑/Z monter | ↓/S descendre | ESPACE sauter | X creuser
 * Contrôles Joueur 2 :  J/L          | I monter  | K descendre   | N sauter      | M creuser
 */
public class GameScene {

    // ── Dimensions ──────────────────────────────────────────────────────────
    private static final int W     = 820;
    private static final int H     = 600;
    private static final int HUD_H = 64;

    // ── États ───────────────────────────────────────────────────────────────
    private enum State { MENU, PLAYING, GAME_OVER }
    private State state = State.MENU;

    // ── JavaFX ───────────────────────────────────────────────────────────────
    private final Scene          scene;
    private final GraphicsContext gc;

    // ── Logique ──────────────────────────────────────────────────────────────
    private final GameEngine     engine;
    private final Set<KeyCode>   keys       = new HashSet<>();
    private long                 frameCount = 0;

    // ── Réseau ───────────────────────────────────────────────────────────────
    private GameServer server;
    private GameClient client;
    private String     networkStatus = "";

    // ── Menu ─────────────────────────────────────────────────────────────────
    private static final String[] MENU_ITEMS = {
        "Jouer (Solo)",
        "Jouer à 2 (Même clavier)",
        "Réseau (prototype) — héberger",
        "Réseau (prototype) — rejoindre localhost",
        "Quitter"
    };
    private int selectedMenuItem = 0;

    public GameScene() {
        engine = new GameEngine();

        Canvas canvas = new Canvas(W, H);
        gc = canvas.getGraphicsContext2D();

        StackPane root = new StackPane(canvas);
        root.setStyle("-fx-background-color: #07071a;");
        scene = new Scene(root, W, H);

        scene.setOnKeyPressed(e -> {
            keys.add(e.getCode());
            handleMenuKey(e.getCode());
            if (state == State.PLAYING) {
                // Creusage joueur 1
                if (e.getCode() == KeyCode.X) engine.playerDig(engine.getPlayer());
                // Creusage joueur 2
                if (e.getCode() == KeyCode.M && engine.isTwoPlayerMode())
                    engine.playerDig(engine.getPlayer2());
            }
            if (e.getCode() == KeyCode.ESCAPE) returnToMenu();
        });
        scene.setOnKeyReleased(e -> keys.remove(e.getCode()));

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (state == State.PLAYING) {
                    processInput();
                    engine.update();
                    if (engine.isGameOver()) {
                        state = State.GAME_OVER;
                        sendScoreToServer();
                    }
                }
                render();
                frameCount++;
            }
        }.start();
    }

    // ── Entrées ──────────────────────────────────────────────────────────────

    private void processInput() {
        Player p1 = engine.getPlayer();

        // Joueur 1 : flèches ou ZQSD
        if (keys.contains(KeyCode.LEFT) || keys.contains(KeyCode.Q)) {
            p1.moveLeft();
        } else if (keys.contains(KeyCode.RIGHT) || keys.contains(KeyCode.D)) {
            p1.moveRight();
        } else {
            p1.stopX();
        }
        if (keys.contains(KeyCode.UP)   || keys.contains(KeyCode.Z)) p1.climbUp();
        else if (keys.contains(KeyCode.DOWN) || keys.contains(KeyCode.S)) p1.climbDown();
        if (keys.contains(KeyCode.SPACE)) p1.jump();

        // Joueur 2 : I/J/K/L + N sauter
        if (engine.isTwoPlayerMode()) {
            Player p2 = engine.getPlayer2();
            if (!p2.isAlive()) return;
            if (keys.contains(KeyCode.J)) {
                p2.moveLeft();
            } else if (keys.contains(KeyCode.L)) {
                p2.moveRight();
            } else {
                p2.stopX();
            }
            if      (keys.contains(KeyCode.I)) p2.climbUp();
            else if (keys.contains(KeyCode.K)) p2.climbDown();
            if (keys.contains(KeyCode.N)) p2.jump();
        }
    }

    private void handleMenuKey(KeyCode code) {
        if (state == State.MENU) {
            switch (code) {
                case UP    -> selectedMenuItem = (selectedMenuItem - 1 + MENU_ITEMS.length) % MENU_ITEMS.length;
                case DOWN  -> selectedMenuItem = (selectedMenuItem + 1) % MENU_ITEMS.length;
                case ENTER, SPACE -> activateMenuItem();
                default -> {}
            }
        } else if (state == State.GAME_OVER) {
            if (code == KeyCode.R) restartGame();
            if (code == KeyCode.ESCAPE || code == KeyCode.M) returnToMenu();
        }
    }

    private void activateMenuItem() {
        switch (selectedMenuItem) {
            case 0 -> startSolo();
            case 1 -> startTwoPlayers();
            case 2 -> startAsServer();
            case 3 -> joinAsClient();
            case 4 -> System.exit(0);
        }
    }

    // ── Actions menu ────────────────────────────────────────────────────────

    private void startSolo() {
        engine.reset(false);
        state = State.PLAYING;
        networkStatus = "Solo — " + engine.getMapName();
    }

    private void startTwoPlayers() {
        engine.reset(true);
        state = State.PLAYING;
        networkStatus = "2 Joueurs — " + engine.getMapName();
    }

    private void startAsServer() {
        engine.reset(false);
        server = new GameServer(5000);
        server.start();
        client = new GameClient("localhost", 5000, this::onServerMessage);
        if (client.connect()) client.join("Hôte");
        networkStatus = "Serveur actif — " + engine.getMapName();
        state = State.PLAYING;
    }

    private void joinAsClient() {
        engine.reset(false);
        client = new GameClient("localhost", 5000, this::onServerMessage);
        if (client.connect()) {
            client.join("Joueur");
            networkStatus = "Connecté à localhost:5000 — " + engine.getMapName();
        } else {
            networkStatus = "Connexion échouée";
        }
        state = State.PLAYING;
    }

    private void onServerMessage(String msg) {
        javafx.application.Platform.runLater(() -> networkStatus = msg);
    }

    private void restartGame() {
        engine.reset(engine.isTwoPlayerMode());
        state = State.PLAYING;
    }

    private void returnToMenu() {
        if (client != null) { client.disconnect(); client = null; }
        if (server != null) { server.stop(); server = null; }
        state = State.MENU;
        networkStatus = "";
    }

    private void sendScoreToServer() {
        if (client != null && client.isConnected()) {
            client.sendScore("Joueur", engine.getPlayer().getScore());
            client.requestScores();
        }
    }

    // ── Rendu ────────────────────────────────────────────────────────────────

    private void render() {
        drawBackground();
        switch (state) {
            case MENU      -> drawMenu();
            case PLAYING   -> { drawGame(); drawHUD(); }
            case GAME_OVER -> { drawGame(); drawHUD(); drawGameOver(); }
        }
    }

    private void drawBackground() {
        LinearGradient bg = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#07071a")),
            new Stop(1, Color.web("#0d0d2b")));
        gc.setFill(bg);
        gc.fillRect(0, 0, W, H);
    }

    // ── Menu ────────────────────────────────────────────────────────────────

    private void drawMenu() {
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 52));
        gc.setFill(Color.web("#ffcc00"));
        gc.fillText("LODE RUNNER", W / 2.0, 110);

        gc.setFont(Font.font("Monospace", 18));
        gc.setFill(Color.web("#aaaacc"));
        gc.fillText("Java 21  ·  JavaFX  ·  Plateforme 2D", W / 2.0, 148);

        gc.setStroke(Color.web("#2255aa", 0.7));
        gc.setLineWidth(2);
        gc.strokeLine(80, 168, W - 80, 168);

        gc.setFont(Font.font("Monospace", 13));
        gc.setFill(Color.web("#556699"));
        gc.fillText("↑ ↓ : naviguer   ENTRÉE : sélectionner", W / 2.0, 192);

        for (int i = 0; i < MENU_ITEMS.length; i++) {
            double iy = 250 + i * 56;
            boolean sel = i == selectedMenuItem;

            if (sel) {
                gc.setFill(Color.web("#1a3a7a", 0.85));
                gc.fillRoundRect(W / 2.0 - 220, iy - 28, 440, 44, 10, 10);
                gc.setStroke(Color.web("#4488ff", 0.9));
                gc.setLineWidth(2);
                gc.strokeRoundRect(W / 2.0 - 220, iy - 28, 440, 44, 10, 10);
                gc.setFill(Color.web("#ffcc00"));
                gc.setFont(Font.font("Monospace", FontWeight.BOLD, 18));
                gc.setTextAlign(TextAlignment.LEFT);
                gc.fillText("▶", W / 2.0 - 208, iy);
                gc.setTextAlign(TextAlignment.CENTER);
            }

            gc.setFont(Font.font("Monospace", FontWeight.BOLD, 18));
            gc.setFill(sel ? Color.web("#ffffff") : Color.web("#8899bb"));
            gc.fillText(MENU_ITEMS[i], W / 2.0, iy);
        }

        // Légende contrôles
        gc.setFont(Font.font("Monospace", 11));
        gc.setFill(Color.web("#445577"));
        gc.fillText("J1: ←→ Z↑ S↓ ESPACE sauter X creuser    J2: J/L I↑ K↓ N sauter M creuser", W / 2.0, H - 40);

        gc.setFont(Font.font("Monospace", 11));
        gc.setFill(Color.web("#334455"));
        gc.fillText("4 cartes  |  Adversaires IA  |  Mode 2 joueurs local", W / 2.0, H - 20);
    }

    // ── Jeu ─────────────────────────────────────────────────────────────────

    private void drawGame() {
        gc.save();
        gc.translate(0, HUD_H);
        drawLevel();
        drawEnemies();
        drawPlayer(engine.getPlayer(), Color.web("#0077ee"), Color.web("#004499"), Color.web("#33aaff"));
        if (engine.isTwoPlayerMode()) {
            drawPlayer(engine.getPlayer2(), Color.web("#ee7700"), Color.web("#994400"), Color.web("#ffaa33"));
        }
        gc.restore();
    }

    private void drawLevel() {
        Level level = engine.getLevel();
        int T = GameEngine.TILE_SIZE;
        for (int x = 0; x < level.getWidth(); x++) {
            for (int y = 0; y < level.getHeight(); y++) {
                Tile t  = level.getTile(x, y);
                double px = x * T, py = y * T;
                switch (t.getType()) {
                    case SOLID       -> drawSolid(px, py, T);
                    case DIG         -> { if (t.isDug()) drawDug(px, py, T);
                                          else           drawDig(px, py, T); }
                    case LADDER      -> drawLadder(px, py, T);
                    case ROPE        -> drawRope(px, py, T);
                    case COLLECTIBLE -> drawGold(px, py, T);
                    default -> {}
                }
            }
        }
    }

    private void drawSolid(double x, double y, int T) {
        gc.setFill(Color.web("#5c3317")); gc.fillRect(x, y, T, T);
        gc.setFill(Color.web("#8b5a2b")); gc.fillRect(x, y, T, 3); gc.fillRect(x, y, 3, T);
        gc.setFill(Color.web("#3a1f0a")); gc.fillRect(x, y+T-3, T, 3); gc.fillRect(x+T-3, y, 3, T);
        gc.setStroke(Color.web("#2a1005", 0.4)); gc.setLineWidth(1);
        gc.strokeLine(x, y+T/2.0, x+T, y+T/2.0);
        gc.strokeLine(x+T*0.5, y, x+T*0.5, y+T/2.0);
    }

    private void drawDig(double x, double y, int T) {
        gc.setFill(Color.web("#7a4a2a")); gc.fillRect(x, y, T, T);
        gc.setFill(Color.web("#a0652d")); gc.fillRect(x, y, T, 2);
        gc.setStroke(Color.web("#4a2a10", 0.5)); gc.setLineWidth(1);
        gc.strokeRect(x+2, y+2, T-4, T-4);
        gc.setStroke(Color.web("#ff9944", 0.4)); gc.setLineWidth(1);
        gc.strokeLine(x+4, y+4, x+T-4, y+T-4);
    }

    private void drawDug(double x, double y, int T) {
        gc.setFill(Color.web("#080818")); gc.fillRect(x, y, T, T);
        gc.setFill(Color.web("#1a0a05", 0.6)); gc.fillRect(x+4, y+6, T-8, T-10);
    }

    private void drawLadder(double x, double y, int T) {
        gc.setFill(Color.web("#39ff14", 0.08)); gc.fillRect(x, y, T, T);
        gc.setStroke(Color.web("#39ff14")); gc.setLineWidth(2.5);
        gc.strokeLine(x+T*0.27, y, x+T*0.27, y+T);
        gc.strokeLine(x+T*0.73, y, x+T*0.73, y+T);
        for (int i = 0; i < 3; i++) {
            double by = y + T * (0.18 + i * 0.30);
            gc.strokeLine(x+T*0.27, by, x+T*0.73, by);
        }
    }

    private void drawRope(double x, double y, int T) {
        double ry = y + T * 0.32;
        gc.setStroke(Color.web("#c8a050")); gc.setLineWidth(3);
        gc.strokeLine(x, ry, x+T, ry);
        gc.setFill(Color.web("#daa520"));
        gc.fillOval(x+T*0.38, ry-3, 6, 6);
    }

    private void drawGold(double x, double y, int T) {
        double pulse = 0.75 + 0.25 * Math.sin(frameCount * 0.07 + x * 0.13);
        int m = 7;
        gc.setFill(Color.color(1.0, 0.82 * pulse, 0.0, 1.0));
        gc.fillRoundRect(x+m, y+m, T-2*m, T-2*m, 5, 5);
        gc.setFill(Color.color(1.0, 1.0, 0.6 * pulse, 0.65));
        gc.fillRoundRect(x+m+2, y+m+2, (T-2*m)*0.55, 3, 2, 2);
        gc.setStroke(Color.color(1.0, 0.88, 0.15, 0.9)); gc.setLineWidth(1.5);
        gc.strokeRoundRect(x+m, y+m, T-2*m, T-2*m, 5, 5);
    }

    private void drawEnemies() {
        int S = GameEngine.ENTITY_SIZE;
        for (Enemy e : engine.getEnemies()) {
            if (e.isBuried()) continue;
            double ex = e.getX(), ey = e.getY();
            gc.setFill(Color.web("#bb1100")); gc.fillRoundRect(ex+2, ey+2, S-4, S-4, 8, 8);
            gc.setFill(Color.web("#880000")); gc.fillRoundRect(ex+4, ey+2, S-8, S/3.0, 5, 5);
            // Yeux qui changent selon la direction ou si sur échelle
            int dir = e.getDirection();
            if (e.isOnLadder()) {
                // Sur l'échelle : yeux regardent vers le haut
                gc.setFill(Color.WHITE); gc.fillOval(ex+S*0.26, ey+S*0.22, 8, 8);
                gc.setFill(Color.BLACK); gc.fillOval(ex+S*0.30, ey+S*0.26, 5, 5);
                gc.setFill(Color.WHITE); gc.fillOval(ex+S*0.60, ey+S*0.22, 8, 8);
                gc.setFill(Color.BLACK); gc.fillOval(ex+S*0.64, ey+S*0.26, 5, 5);
            } else {
                gc.setFill(Color.WHITE); gc.fillOval(ex+(dir>0?S*0.54:S*0.18), ey+S*0.28, 8, 8);
                gc.setFill(Color.BLACK); gc.fillOval(ex+(dir>0?S*0.63:S*0.22), ey+S*0.32, 5, 5);
            }
            gc.setStroke(Color.web("#ff3311")); gc.setLineWidth(1.5);
            gc.strokeRoundRect(ex+2, ey+2, S-4, S-4, 8, 8);
        }
    }

    private void drawPlayer(Player p, Color body, Color helm, Color glow) {
        if (!p.isAlive()) return;
        double px = p.getX(), py = p.getY();
        int S = GameEngine.ENTITY_SIZE;
        gc.setFill(body); gc.fillRoundRect(px+2, py+2, S-4, S-4, 8, 8);
        gc.setFill(helm); gc.fillRoundRect(px+4, py+2, S-8, S/3.0, 5, 5);
        gc.setFill(Color.WHITE); gc.fillOval(px+(p.isFacingRight()?S*0.54:S*0.18), py+S*0.28, 8, 8);
        gc.setFill(Color.web("#001166")); gc.fillOval(px+(p.isFacingRight()?S*0.63:S*0.22), py+S*0.32, 5, 5);
        gc.setStroke(glow.deriveColor(0, 1, 1, 0.75)); gc.setLineWidth(2);
        gc.strokeRoundRect(px+1, py+1, S-2, S-2, 9, 9);
        if (Math.abs(p.getVelocityX()) > 0.1) {
            double leg = Math.sin(frameCount * 0.25) * 4;
            gc.setFill(body.darker());
            gc.fillRect(px + S*0.25, py + S*0.72, 5, 6 + leg);
            gc.fillRect(px + S*0.62, py + S*0.72, 5, 6 - leg);
        }
        // Indicateur si sur échelle
        if (p.isOnLadder()) {
            gc.setStroke(Color.web("#39ff14", 0.6)); gc.setLineWidth(1.5);
            gc.strokeLine(px + S/2.0, py - 4, px + S/2.0, py - 1);
        }
    }

    // ── HUD ─────────────────────────────────────────────────────────────────

    private void drawHUD() {
        gc.setFill(Color.web("#04040f", 0.94));
        gc.fillRect(0, 0, W, HUD_H);
        gc.setStroke(Color.web("#2255aa", 0.7)); gc.setLineWidth(2);
        gc.strokeLine(0, HUD_H-1, W, HUD_H-1);

        // Titre
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 17));
        gc.setFill(Color.web("#ffcc00"));
        gc.fillText("LODE RUNNER", 10, 22);

        if (engine.isTwoPlayerMode()) {
            // Mode 2 joueurs : score J1 et J2
            Player p1 = engine.getPlayer(), p2 = engine.getPlayer2();

            gc.setFill(Color.web("#33aaff"));
            gc.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
            gc.fillText("J1  SCR:" + p1.getScore(), 10, 44);
            String h1 = "♥".repeat(Math.max(0, p1.getLives()));
            gc.setFill(Color.web("#ff5555")); gc.fillText(h1, 120, 44);

            gc.setFill(Color.web("#ffaa33"));
            gc.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
            gc.fillText("J2  SCR:" + p2.getScore(), 250, 44);
            String h2 = "♥".repeat(Math.max(0, p2.getLives()));
            gc.setFill(Color.web("#ff5555")); gc.fillText(h2, 360, 44);

            // Or restant
            gc.setFill(Color.web("#ffd700"));
            gc.setFont(Font.font("Monospace", 14));
            gc.fillText("OR: " + engine.getLevel().countGold(), 510, 44);

            // Statut réseau / map
            gc.setFill(Color.web("#44aa44"));
            gc.setFont(Font.font("Monospace", 11));
            gc.fillText(networkStatus, 10, 57);

            // Aide contrôles J2
            gc.setFill(Color.web("#ffffff", 0.25));
            gc.setFont(Font.font("Monospace", 10));
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.fillText("J1:←→ Z↑ S↓ SPC saut X dig   |   J2: J/L I↑ K↓ N saut M dig", W - 8, 57);
        } else {
            // Mode solo
            gc.setFill(Color.web("#00ccff"));
            gc.setFont(Font.font("Monospace", FontWeight.BOLD, 17));
            gc.fillText("SCORE: " + engine.getPlayer().getScore(), 190, 22);

            gc.setFill(Color.web("#ff5555"));
            String hearts = "VIES: " + "♥ ".repeat(Math.max(0, engine.getPlayer().getLives()));
            gc.fillText(hearts, 380, 22);

            gc.setFill(Color.web("#ffd700"));
            gc.setFont(Font.font("Monospace", 14));
            gc.fillText("OR: " + engine.getLevel().countGold(), 640, 22);

            gc.setFill(Color.web("#44aa44"));
            gc.setFont(Font.font("Monospace", 11));
            gc.fillText(networkStatus, 10, 44);

            gc.setFill(Color.web("#ffffff", 0.3));
            gc.setFont(Font.font("Monospace", 11));
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.fillText("←→ déplacer  Z↑ échelle  ESPACE sauter  X creuser  ECHAP menu", W - 8, 44);

            // Map name
            gc.setFill(Color.web("#334466"));
            gc.setFont(Font.font("Monospace", 10));
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.fillText(engine.getMapName(), W - 8, 57);
        }
        gc.setTextAlign(TextAlignment.LEFT); // reset
    }

    // ── Game Over ────────────────────────────────────────────────────────────

    private void drawGameOver() {
        gc.setFill(Color.color(0, 0, 0, 0.80));
        gc.fillRect(0, 0, W, H);

        boolean p2won = engine.isTwoPlayerMode() && engine.getPlayer2().isAlive()
                        && !engine.getPlayer().isAlive();

        String msg   = engine.isVictory() ? "VICTOIRE !" :
                       p2won ? "JOUEUR 2 GAGNE !" : "GAME OVER";
        Color  color = engine.isVictory() ? Color.web("#ffcc00") :
                       p2won ? Color.web("#ffaa33") : Color.web("#ff3322");

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 52));
        gc.setFill(color);
        gc.fillText(msg, W / 2.0, H / 2.0 - 50);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 22));
        if (engine.isTwoPlayerMode()) {
            gc.fillText("J1: " + engine.getPlayer().getScore() + " pts  |  J2: "
                + engine.getPlayer2().getScore() + " pts", W / 2.0, H / 2.0 + 5);
        } else {
            gc.fillText("Score : " + engine.getPlayer().getScore() + " pts", W / 2.0, H / 2.0 + 5);
        }

        drawButton(W / 2.0 - 120, H / 2.0 + 50, 220, 42, "R  —  Rejouer",  Color.web("#1a4a1a"), Color.web("#44ff44"));
        drawButton(W / 2.0 - 120, H / 2.0 + 105, 220, 42, "ECHAP — Menu", Color.web("#1a1a4a"), Color.web("#4488ff"));

        if (!networkStatus.isEmpty() && networkStatus.startsWith("SCORES")) {
            gc.setFont(Font.font("Monospace", 14));
            gc.setFill(Color.web("#aaaaff"));
            gc.fillText("Classement réseau :", W / 2.0, H / 2.0 + 180);
            gc.setFill(Color.WHITE);
            gc.fillText(networkStatus.replace("SCORES ", "").replace(",", "  "), W / 2.0, H / 2.0 + 204);
        }
    }

    private void drawButton(double x, double y, double w, double h,
                            String label, Color bg, Color border) {
        gc.setFill(bg);
        gc.fillRoundRect(x, y, w, h, 8, 8);
        gc.setStroke(border); gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, w, h, 8, 8);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 15));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(label, x + w / 2.0, y + h / 2.0 + 5);
    }

    public Scene getScene() { return scene; }
}
