package com.loderunner;

import javafx.application.Application;
import javafx.stage.Stage;
import com.loderunner.ui.GameScene;
import com.loderunner.network.GameServer;

/**
 * Point d'entrée de l'application Lode Runner.
 *
 * Modes de lancement :
 *   java -cp ... com.loderunner.Main            → interface graphique (normal)
 *   java -cp ... com.loderunner.Main --server   → serveur headless (sans UI)
 *
 * En mode normal, le joueur peut aussi héberger / rejoindre depuis le menu.
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) {
        GameScene game = new GameScene();
        stage.setScene(game.getScene());
        stage.setTitle("Lode Runner");
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        // Mode serveur headless (sans interface graphique)
        for (String arg : args) {
            if ("--server".equals(arg)) {
                System.out.println("=== Lode Runner — Serveur headless ===");
                GameServer srv = new GameServer(5000);
                srv.start();
                // Garde le thread principal en vie
                try { Thread.currentThread().join(); }
                catch (InterruptedException e) { srv.stop(); }
                return;
            }
        }
        // Mode normal : interface graphique JavaFX
        launch(args);
    }
}
