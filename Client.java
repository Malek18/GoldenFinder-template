package com.example.goldfinder;

import javafx.application.Platform;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Client {
    private Socket client;
    private final String playerName;
    private final Controller controller;
    private final AppClient appClient;
    private InputStream input;
    private OutputStream output;
    private ClientHandler clientHandler;
    private final ArrayList<String> playerPos = new ArrayList<>();
    private final ArrayList<String> botPos = new ArrayList<>();
    String endingString;


    public Client(String playerName, Controller controller, AppClient appClient){
        this.playerName = playerName;
        this.controller = controller;
        this.appClient = appClient;
        controller.setClient(this);
    }

    // Connection du client avec le serveur
    public void connect(String ip, int port){
        try {
            client = new Socket(ip, port);
            input = client.getInputStream();
            output = client.getOutputStream();

            byte[] buffer = new byte[16384];

            output.write(("GAME_JOIN:" + playerName).getBytes());
            controller.gridView.paintToken(10, 10);
            int bytes = input.read(buffer);
            String gameStart = new String(buffer, 0, bytes);
            if (gameStart.contains("GAME_START")){
                startGame();
            }
            else{
                System.out.println("Connexion error");
            }

        } catch (IOException e) {
            appClient.echoMessage("Error on ip or port");
            System.exit(1);
        }
    }

    private void startGame() throws IOException {
        byte[] buffer = new byte[16384];
        int bytes = input.read(buffer);
        String pos = new String(buffer, 0, bytes);

        System.out.println(pos);

        if (pos.startsWith("POSITION:")){
            //Recuperer la position
            pos = pos.substring(9);
            int posVirg = pos.indexOf(',');

            controller.column = Integer.parseInt(pos.substring(0, posVirg));
            controller.row = Integer.parseInt(pos.substring(posVirg + 1));

            //
            surrounding();
            buffer = new byte[16384];
            bytes = input.read(buffer);
            updateMap(new String(buffer, 0, bytes));
            refresh();

            appClient.setKeyPressed();

            clientHandler = new ClientHandler(this);
            (new Thread(clientHandler)).start();

            excutorService(10);
        }
        else {
            System.out.println("ERROR on POSITION");
        }
    }

    public void move(String dir) throws IOException {
        output.write(dir.getBytes());
        clientHandler.changeMove(dir);
    }

    public void moveUP(String message) throws IOException {
        controller.row--;
        manageGold(message);

        //surrounding
        surrounding();
    }

    public void moveRIGHT(String message) throws IOException {
        controller.column++;
        manageGold(message);

        //surrounding
        surrounding();
    }

    public void moveDOWN(String message) throws IOException {
        controller.row++;
        manageGold(message);

        //surrounding
        surrounding();
    }

    public void moveLEFT(String message) throws IOException {
        controller.column--;
        manageGold(message);

        //surrounding
        surrounding();
    }
 // Recuperer la piece Gold
    private void manageGold(String move){
        if (move.contains("GOLD")){
            Platform.runLater(() -> {
                controller.gridView.goldAt[controller.column][controller.row] = false;
                controller.incrementGold();
            });
        }
    }

    private void surrounding() throws IOException {
        output.write("SURROUNDING".getBytes());
    }

    public void updateMap(String surrounding){
        playerPos.clear();
        botPos.clear();
        System.out.println("----------------------------------------------\n" + surrounding);

        surrounding = surrounding.substring(3);
        int indexOfSpace = surrounding.indexOf(' ');
        String up = surrounding.substring(0, indexOfSpace);

        surrounding = surrounding.substring(indexOfSpace + 7);
        indexOfSpace = surrounding.indexOf(' ');
        String right = surrounding.substring(0, indexOfSpace);

        surrounding = surrounding.substring(indexOfSpace + 6);
        indexOfSpace = surrounding.indexOf(' ');
        String down = surrounding.substring(0, indexOfSpace);

        surrounding = surrounding.substring(indexOfSpace + 6);
        indexOfSpace = surrounding.indexOf(' ');
        String left = surrounding.substring(0, indexOfSpace);

        switch (up) {
            case "WALL" :
                controller.gridView.hWall[controller.column][controller.row] = true;
                break;
            case "GOLD" :
                controller.gridView.goldAt[controller.column][controller.row - 1] = true;
                break;
            case "EMPTY" :
                controller.gridView.goldAt[controller.column][controller.row - 1] = false;
                break;
        }
        if (up.contains("PLAYER")) {
            playerPos.add("UP");
            controller.gridView.goldAt[controller.column][controller.row - 1] = false;
        }
        else if (up.contains("BOT")) {
            botPos.add("UP");
            controller.gridView.goldAt[controller.column][controller.row - 1] = false;
        }

        switch (right) {
            case "WALL" :
                controller.gridView.vWall[controller.column + 1][controller.row] = true;
                break;
            case "GOLD" :
                controller.gridView.goldAt[controller.column + 1][controller.row] = true;
                break;
            case "EMPTY" :
                controller.gridView.goldAt[controller.column + 1][controller.row] = false;
                break;
        }
        if (right.contains("PLAYER")) {
            playerPos.add("RIGHT");
            controller.gridView.goldAt[controller.column + 1][controller.row] = false;
        }
        else if (right.contains("BOT")) {
            botPos.add("RIGHT");
            controller.gridView.goldAt[controller.column + 1][controller.row] = false;
        }

            switch (down) {
            case "WALL" :
                controller.gridView.hWall[controller.column][controller.row + 1] = true;
                break;
            case "GOLD" :
                controller.gridView.goldAt[controller.column][controller.row + 1] = true;
                break;
            case "EMPTY" :
                controller.gridView.goldAt[controller.column][controller.row + 1] = false;
                break;
        }
        if (down.contains("PLAYER")) {
            playerPos.add("DOWN");
            controller.gridView.goldAt[controller.column][controller.row + 1] = false;
        }
        else if (down.contains("BOT")) {
            botPos.add("DOWN");
            controller.gridView.goldAt[controller.column][controller.row + 1] = false;
        }

            switch (left) {
            case "WALL":
                controller.gridView.vWall[controller.column][controller.row] = true;
                break;
            case "GOLD":
                controller.gridView.goldAt[controller.column - 1][controller.row] = true;
                break;
            case "EMPTY":
                controller.gridView.goldAt[controller.column - 1][controller.row] = false;
                break;
        }
        if (left.contains("PLAYER")) {
            playerPos.add("LEFT");
            controller.gridView.goldAt[controller.column - 1][controller.row] = false;
        }
        else if (left.contains("BOT")) {
            botPos.add("LEFT");
            controller.gridView.goldAt[controller.column - 1][controller.row] = false;
        }
    }

    public void updatePlayer(String message) throws IOException {
        String[] string = message.split(":");
        if (string[1].contains("PLAYER")){
            playerPos.add(string[0]);
        }
        surrounding();
    }

    public void updateBot(String message) throws IOException {
        String[] string = message.split(":");
        if (string[1].contains("BOT")){
            botPos.add(string[0]);
        }
        surrounding();
    }

    public void refresh(){
        controller.gridView.repaint();
        controller.gridView.paintToken(controller.column, controller.row);

        for (String pos : playerPos){
            switch (pos){
                case "UP" :
                    controller.gridView.paintPlayerToken(controller.column, controller.row - 1);
                    break;
                case "RIGHT" :
                    controller.gridView.paintPlayerToken(controller.column + 1, controller.row);
                    break;
                case "DOWN" :
                    controller.gridView.paintPlayerToken(controller.column, controller.row + 1);
                    break;
                case "LEFT" :
                    controller.gridView.paintPlayerToken(controller.column - 1, controller.row);
                    break;
            }
        }
        playerPos.clear();

        for (String pos : botPos){
            switch (pos){
                case "UP" :
                    controller.gridView.paintBotToken(controller.column, controller.row - 1);
                    break;
                case "RIGHT" :
                    controller.gridView.paintBotToken(controller.column + 1, controller.row);
                    break;
                case "DOWN" :
                    controller.gridView.paintBotToken(controller.column, controller.row + 1);
                    break;
                case "LEFT" :
                    controller.gridView.paintBotToken(controller.column - 1, controller.row);
                    break;
            }
        }
        botPos.clear();
    }

    private void askLeaders() throws IOException {
        output.write("LEADER:5".getBytes());
        System.out.println("LEADER send");
    }

    public void ending() throws IOException {
        Platform.runLater(() -> {
            appClient.echoMessage(endingString);
            appClient.ending(endingString);
        });
    }

    public void close() throws IOException {
        this.client.close();
    }

    public InputStream getInput(){ return this.input; }

    // methode pour planifier l'interrogation du leader
    private void excutorService(int periode){
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        try {
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    askLeaders();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, 0, periode, TimeUnit.SECONDS);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public void echoMessage(String message){
        System.out.println(message);
    }
}
