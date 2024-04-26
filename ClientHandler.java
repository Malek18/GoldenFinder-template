package com.example.goldfinder;

import java.io.IOException;
import java.io.InputStream;

public class ClientHandler extends Thread{
    private final Client client;
    private final InputStream input;
    private String lastMove;

    public ClientHandler(Client client){
        this.client = client;
        this.input = client.getInput();
    }

    public void changeMove(String newMove){ this.lastMove = newMove; }

    public void run(){
        byte[] buffer = new byte[16384];
        int bytes;
        try {
            while ((bytes = input.read(buffer)) != -1) {
                String message = new String(buffer, 0, bytes);
                // réponse d'une dirrection
                if (message.contains("VALIDMOVE")){
                    if (!(message.compareTo("INVALIDMOVE") == 0)){
                        switch (lastMove){
                            case "UP" :
                                client.moveUP(message);
                                break;
                            case "RIGHT" :
                                client.moveRIGHT(message);
                                break;
                            case "DOWN" :
                                client.moveDOWN(message);
                                break;
                            case "LEFT" :
                                client.moveLEFT(message);
                                break;
                        }
                        client.refresh();
                    }
                }

                //surrounding or update player
                else if (message.contains("UP") || message.contains("RIGHT") || message.contains("DOWN") || message.contains("LEFT")) {
                    //surrounding
                    if (message.contains("UP") && message.contains("RIGHT")){
                        client.updateMap(message);
                        client.echoMessage("surrounding receive");
                    }
                    //dir:PLAYER or dir:EMPTY
                    else if (message.contains("PLAYER")){
                        client.updatePlayer(message);
                    }
                    else if (message.contains("BOT")){
                        client.updateBot(message);
                    }
                    client.refresh();
                }

                //SCORE:
                else if (message.startsWith("SCORE:")){
                    client.echoMessage(message);
                }

                //GAME_END
                else if (message.contains("GAME_END")) {
                    client.endingString = message;
                    client.ending();
                    client.close();
                }
            }
        } catch (IOException e) {
            try {
                client.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
