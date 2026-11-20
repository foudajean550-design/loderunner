package com.loderunner.network;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Serveur TCP multijoueur pour Lode Runner.
 *
 * Protocole texte (une commande par ligne) :
 *   Client → Serveur :  JOIN <pseudo>
 *                       ACTION <LEFT|RIGHT|JUMP|DIG|STOP>
 *                       SCORE <pseudo> <valeur>
 *                       GET_SCORES
 *                       QUIT
 *
 *   Serveur → Client :  WELCOME <pseudo>
 *                       SCORES <pseudo1>:<score1>,<pseudo2>:<score2>,...
 *                       BROADCAST <message>
 *                       ERROR <message>
 *
 * Utilisation :
 *   GameServer srv = new GameServer(5000);
 *   srv.start();   // non bloquant, lance un thread dédié
 *   srv.stop();
 */
public class GameServer {

    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean running;

    /** Scores persistants : pseudo → meilleur score. */
    private final Map<String, Integer> scores = new ConcurrentHashMap<>();

    /** Clients connectés. */
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    /** Fichier de persistance des scores. */
    private static final String SCORE_FILE = "scores.dat";

    public GameServer(int port) {
        this.port = port;
        loadScores();
    }

    /** Lance le serveur dans un thread daemon (non bloquant). */
    public void start() {
        running = true;
        Thread t = new Thread(this::acceptLoop, "GameServer-accept");
        t.setDaemon(true);
        t.start();
        System.out.println("[Serveur] Démarré sur le port " + port);
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); }
        catch (IOException ignored) {}
        saveScores();
        System.out.println("[Serveur] Arrêté.");
    }

    /** Boucle d'acceptation des connexions entrantes. */
    private void acceptLoop() {
        try {
            serverSocket = new ServerSocket(port);
            while (running) {
                try {
                    Socket sock = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(sock, this);
                    clients.add(handler);
                    Thread ct = new Thread(handler, "Client-" + sock.getInetAddress());
                    ct.setDaemon(true);
                    ct.start();
                    System.out.println("[Serveur] Nouveau client : " + sock.getInetAddress());
                } catch (IOException e) {
                    if (running) System.err.println("[Serveur] Erreur accept : " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (running) System.err.println("[Serveur] Impossible d'ouvrir le port " + port);
        }
    }

    /** Envoie un message à tous les clients connectés. */
    public void broadcast(String message) {
        for (ClientHandler c : clients) c.send("BROADCAST " + message);
    }

    /** Enregistre ou met à jour le score d'un joueur (garde le meilleur). */
    public synchronized void updateScore(String pseudo, int score) {
        int best = scores.getOrDefault(pseudo, 0);
        if (score > best) {
            scores.put(pseudo, score);
            saveScores();
            broadcast(pseudo + " a réalisé un nouveau record : " + score + " pts !");
        }
    }

    /** Retourne le classement formaté pour envoi au client. */
    public String getScoresFormatted() {
        StringBuilder sb = new StringBuilder("SCORES ");
        scores.entrySet().stream()
              .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
              .forEach(e -> sb.append(e.getKey()).append(":").append(e.getValue()).append(","));
        if (sb.charAt(sb.length() - 1) == ',')
            sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public void removeClient(ClientHandler c) { clients.remove(c); }

    // ── Persistance ────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void loadScores() {
        File f = new File(SCORE_FILE);
        if (!f.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            Map<String, Integer> loaded = (Map<String, Integer>) ois.readObject();
            scores.putAll(loaded);
            System.out.println("[Serveur] Scores chargés : " + scores.size() + " entrées.");
        } catch (Exception e) {
            System.err.println("[Serveur] Impossible de charger les scores : " + e.getMessage());
        }
    }

    private void saveScores() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SCORE_FILE))) {
            oos.writeObject(new HashMap<>(scores));
        } catch (IOException e) {
            System.err.println("[Serveur] Impossible de sauvegarder les scores : " + e.getMessage());
        }
    }

    public int getPort() { return port; }
}
