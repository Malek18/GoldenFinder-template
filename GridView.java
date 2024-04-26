package com.example.goldfinder;

import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;

public class GridView {
    Canvas canvas;
    int columnCount, rowCount;
    boolean[][] goldAt, vWall, hWall;


    public GridView(Canvas canvas, int columnCount, int rowCount) {
        this.canvas = canvas;
        this.columnCount = columnCount;
        this.rowCount = rowCount;
        goldAt = new boolean[columnCount][rowCount];
        vWall = new boolean[columnCount+1][rowCount];
        hWall = new boolean[columnCount][rowCount+1];
    }

    public void repaint(){
        /*
        Actualise l'affichage du labyrinthe

        1 - Actualise celui des pièces
        2 - Actualise celui des murs
         */

        canvas.getGraphicsContext2D().clearRect(0,0,canvas.getWidth(),canvas.getHeight());
        for(int column =0; column<columnCount;column++)
            for(int row=0;row<rowCount;row++)
                if(goldAt[column][row]) {
                    canvas.getGraphicsContext2D().setFill(Color.YELLOW);
                    canvas.getGraphicsContext2D().fillOval(column * cellWidth(), row * cellHeight(), cellWidth(), cellHeight());
                }

        canvas.getGraphicsContext2D().setStroke(Color.WHITE);
        for(int column =0; column<columnCount;column++)
            for(int row=0;row<rowCount;row++){
                    if(vWall[column][row])
                        canvas.getGraphicsContext2D().strokeLine(column * cellWidth(), row * cellHeight(),column * cellWidth(), (row+1) * cellHeight());
                if(hWall[column][row])
                    canvas.getGraphicsContext2D().strokeLine(column * cellWidth(), row * cellHeight(),(column+1) * cellWidth(), row * cellHeight());
            }

    }

    private double cellWidth(){ return canvas.getWidth()/columnCount; }
    private double cellHeight(){ return canvas.getHeight()/rowCount; }

    public void paintToken(int column, int row) {
        //Colorie la case "column" "row" en bleu
        canvas.getGraphicsContext2D().setFill(Color.BLUE);
        canvas.getGraphicsContext2D().fillRect(column*cellWidth(),row*cellHeight(),cellWidth(),cellHeight());
    }

    public void paintPlayerToken(int column, int row){
        //Colorie la case "column" "row" en vert(correspondant à un autre joueur)
        canvas.getGraphicsContext2D().setFill(Color.GREEN);
        canvas.getGraphicsContext2D().fillRect(column*cellWidth(),row*cellHeight(),cellWidth(),cellHeight());
    }

    public void paintBotToken(int column, int row){
        //Colorie la case "column" "row" en rouge(correspondant à un bot)
        canvas.getGraphicsContext2D().setFill(Color.RED);
        canvas.getGraphicsContext2D().fillRect(column*cellWidth(),row*cellHeight(),cellWidth(),cellHeight());
    }
}
