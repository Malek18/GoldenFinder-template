package com.example.goldfinder.server;

public class Position {
    private int x; //horizontal
    private int y; //vertical
    public Position(int x, int y){ this.x = x; this.y = y; }
    public int getX(){ return this.x; }
    public int getY(){ return this.y; }

    private void setX(int x){ this.x = x; }
    private void setY(int y){ this.y = y; }

    public void move(String pos){
        switch (pos){
            case "UP":
                this.setY(this.getY() - 1);
                break;
            case "RIGHT":
                this.setX(this.getX() + 1);
                break;
            case "DOWN":
                this.setY(this.getY() + 1);
                break;
            case "LEFT":
                this.setX(this.getX() - 1);
                break;
        }
    }

    public String toString(){
        return "X:" + x + " Y:" + y;
    }
}