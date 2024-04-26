package com.example.goldfinder;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class PlayerBox extends HBox {
    private final Label classement, playerName, score;

    public PlayerBox(int classement,String playerName,int score){
        this.classement = new Label(classement + "");
        this.playerName = new Label(playerName);
        this.score = new Label(score + "");
        initialize();
    }
    public void initialize(){
        this.getChildren().addAll(classement, playerName, score);
        this.setSpacing(150);
    }
}
