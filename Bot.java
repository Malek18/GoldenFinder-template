package com.example.goldfinder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.*;


public class Bot extends Thread {

    private final String ip;
    private final int port;
    private final String[] directions = {"UP", "DOWN", "LEFT", "RIGHT"};
    private OutputStream output;
    private InputStream input;
    private final String botName;
    private final Random random;

    public Bot(String ip, int port, String botName) {
        this.ip = ip;
        this.port = port;
        this.botName = botName;
        this.random = new Random();
    }

    @Override
    public void run() {
        try {
            // Initialize
            Socket socket = new Socket(ip, port);
            output = socket.getOutputStream();
            input = socket.getInputStream();

            // GAME_JOIN
            output.write(("GAME_JOIN:" + botName).getBytes());

            // Wait GAME_START
            String gameStart = readMessage();
            if (gameStart.contains("GAME_START")){
                String position = readMessage();
                if (position.startsWith("POSITION:")){

                    // Launch Timeline
                    startTicks(500);
                }
            }
            else {
                System.out.println("Connexion error");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Methode qui lit un message entrant et le convertit en chaine de caracteres
    private String readMessage() throws IOException {
        // Creation d'un tableau de bytes pour stocker les données lues
        byte[] buffer = new byte[16384];
        // Lecture des données entrants et stockage de bytes lus
        int bytesRead = input.read(buffer);
        //Conversion des bytes lus en une chaine de caracteres
        // Retour du resultat
        return new String(buffer, 0, bytesRead);
    }

    private void randomMove() throws IOException {
        //Generation d'une position aleatoire
        String randomDirection = this.directions[random.nextInt(directions.length)];
        output.write(randomDirection.getBytes());
    }

    //Methode qui fais des actions periodiques à intervalle regulier
    private void startTicks(int periode){
        Timer timer = new Timer();
        //Planification d'une tache periodique avec un delai initial aleatoire
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try{
                    randomMove();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 0, periode + random.nextInt(801));
    }
}
