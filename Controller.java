package com.example.goldfinder;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;

import java.io.IOException;

import static com.example.goldfinder.server.AppServer.COLUMN_COUNT;
import static com.example.goldfinder.server.AppServer.ROW_COUNT;

public class Controller {

    @FXML
    Canvas gridCanvas;
    @FXML
    Label score;

    GridView gridView;
    int column, row;

    private Client client;

    public void initialize() {
        this.gridView = new GridView(gridCanvas, COLUMN_COUNT, ROW_COUNT);
        score.setText("0");
        gridView.repaint();
        column = 10; row = 10;
        gridView.paintToken(column, row);
    }

    public void pauseToggleButtonAction(ActionEvent actionEvent) {
    }

    public void playToggleButtonAction(ActionEvent actionEvent) {
    }

    public void oneStepButtonAction(ActionEvent actionEvent) {
    }

    public void restartButtonAction(ActionEvent actionEvent) {
    }

    public void handleMove(KeyEvent keyEvent){
        String dir = switch (keyEvent.getCode()) {
            case Z -> "UP";
            case Q -> "LEFT";
            case S -> "DOWN";
            case D -> "RIGHT";
            default -> "";
        };
        try {
            client.move(dir);
        } catch (IOException e) {

        }
//        gridView.repaint();
//        gridView.paintToken(column, row);
    }

    public void setClient(Client client){ this.client = client; }
    public void incrementGold(){ Platform.runLater(() -> score.setText(String.valueOf(Integer.parseInt(score.getText()) + 1)));}
}

