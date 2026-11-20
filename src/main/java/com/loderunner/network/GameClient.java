package com.loderunner.network;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Client TCP se connectant au serveur de jeu.
 *
 * Utilisation :
 *   GameClient client = new GameClient("localhost", 5000, msg -> System.out.println(msg));
 *   client.connect();
 *   client.join("Alice");
 *   client.sendAction("LEFT");
 *   client.sendScore("Alice", 150);
 *   client.requestScores();
 *   client.disconnect();
 */
public class GameClient {

    private final String           host;
    private final int              port;
    private final Consumer<String> onMessage; // callback UI pour les messages reçus

    private Socket       socket;
    private PrintWriter  out;
    private boolean      connected;

    public GameClient(String host, int port, Consumer<String> onMessage) {
        this.host      = host;
        this.port      = port;
        this.onMessage = onMessage;
    }

    /**
     * Établit la connexion TCP et lance le thread d'écoute.
     * @return true si la connexion a réussi
     */
    public boolean connect() {
        try {
            socket    = new Socket(host, port);
            out       = new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            connected = true;

            // Thread d'écoute des messages serveur (daemon : s'arrête avec l'app)
            Thread listener = new Thread(this::listenLoop, "GameClient-listener");
            listener.setDaemon(true);
            listener.start();

            System.out.println("[Client] Connecté à " + host + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("[Client] Connexion échouée : " + e.getMessage());
            return false;
        }
    }

    /** Boucle d'écoute : transmet chaque ligne reçue au callback onMessage. */
    private void listenLoop() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (onMessage != null) onMessage.accept(line);
            }
        } catch (IOException e) {
            if (connected) System.out.println("[Client] Déconnecté du serveur.");
        } finally {
            connected = false;
        }
    }

    /** Envoie le pseudo du joueur pour rejoindre la partie. */
    public void join(String pseudo) { send("JOIN " + pseudo); }

    /** Envoie une action de jeu (LEFT, RIGHT, JUMP, DIG, STOP). */
    public void sendAction(String action) { send("ACTION " + action); }

    /** Envoie le score en fin de partie. */
    public void sendScore(String pseudo, int score) {
        send("SCORE " + pseudo + " " + score);
    }

    /** Demande le classement des scores au serveur. */
    public void requestScores() { send("GET_SCORES"); }

    public void disconnect() {
        connected = false;
        send("QUIT");
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    private synchronized void send(String message) {
        if (out != null && connected) out.println(message);
    }

    public boolean isConnected() { return connected; }
    public String  getHost()     { return host; }
    public int     getPort()     { return port; }
}
