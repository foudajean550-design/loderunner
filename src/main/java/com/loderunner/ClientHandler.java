package com.loderunner.network;

import java.io.*;
import java.net.Socket;

/**
 * Gère la communication avec un client connecté au serveur.
 * Un thread par client (Runnable).
 */
public class ClientHandler implements Runnable {

    private final Socket     socket;
    private final GameServer server;
    private PrintWriter      out;
    private String           pseudo = "Anonyme";

    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        ) {
            out = new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

            String line;
            while ((line = in.readLine()) != null) {
                handleCommand(line.trim());
            }
        } catch (IOException e) {
            System.out.println("[Serveur] Client déconnecté : " + pseudo);
        } finally {
            server.removeClient(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void handleCommand(String line) {
        if (line.startsWith("JOIN ")) {
            pseudo = line.substring(5).trim();
            send("WELCOME " + pseudo);
            server.broadcast(pseudo + " a rejoint la partie.");

        } else if (line.startsWith("SCORE ")) {
            // Format : SCORE <pseudo> <valeur>
            String[] parts = line.split(" ");
            if (parts.length == 3) {
                try {
                    int val = Integer.parseInt(parts[2]);
                    server.updateScore(parts[1], val);
                } catch (NumberFormatException ignored) {}
            }

        } else if (line.equals("GET_SCORES")) {
            send(server.getScoresFormatted());

        } else if (line.equals("QUIT")) {
            server.broadcast(pseudo + " a quitté la partie.");

        } else if (line.startsWith("ACTION ")) {
            // En mode multijoueur complet, retransmettre l'action aux autres clients
            server.broadcast("ACTION " + pseudo + " " + line.substring(7));
        }
    }

    /** Envoie un message au client (thread-safe). */
    public synchronized void send(String message) {
        if (out != null) out.println(message);
    }

    public String getPseudo() { return pseudo; }
}
