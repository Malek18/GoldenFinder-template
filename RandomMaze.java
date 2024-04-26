package com.example.goldfinder.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomMaze {
    int columnCount, rowCount;
    final double openings;      //indice d'ouvrabilité des cases (a quelle point les cases on des murs ou pas)
    boolean[][] hWall;          //tableau des murs horizontaux (si une case a des murs horizontaux)
    boolean[][] vWall;          //tableau des murs verticaux (si une case a des murs verticaux)
    Tile[][] next;              //pour chaque indice (column, row), savoir qu'elle case est la prochaine
    boolean[][] decided;        //savoir si une case est déjà instancié
    int undecidedCount;         //nombre de case qui ne sont pas encore dans decided
    private final Random random;

    public RandomMaze(int columnCount, int rowCount, double openings, Random random) {
        this.columnCount = columnCount;
        this.rowCount = rowCount;
        this.openings = openings;
        this.random = random;
        hWall = new boolean[columnCount][];
        vWall = new boolean[columnCount][];
        for(int column=0; column<columnCount;column++) {
            hWall[column] = new boolean[rowCount];
            vWall[column] = new boolean[rowCount];
        }

    }

    private Tile randomAdjacent(Tile tile){
        //Retourne une des Tile (cellule) aléatoire autour de celle passé en paramètre
        int column= tile.column;
        int row= tile.row;
        List<Tile> adjacent = new ArrayList<>();
        if(column>0)
            adjacent.add(new Tile(column-1,row));
        if(row>0)
            adjacent.add(new Tile(column,row-1));
        if(column<columnCount-1)
            adjacent.add(new Tile(column+1,row));
        if(row<rowCount-1)
            adjacent.add(new Tile(column,row+1));
        return adjacent.get(random.nextInt(adjacent.size()));
    }

    public void generate(){
        /*
            Génère tout les murs du labyrinthe de sorte a ce que toutes les cases soit accessible
         */

        decided = new boolean[columnCount][];
        next = new Tile[columnCount][];
        for(int column=0; column<columnCount;column++) {
            decided[column] = new boolean[rowCount];
            next[column] = new Tile[rowCount];
        }
        undecidedCount = columnCount*rowCount;
        int initCol = random.nextInt(columnCount);
        int initRow = random.nextInt(rowCount);
        decided[initCol][initRow]=true;
        next[initCol][initRow]= new Tile(initCol,initRow);
        undecidedCount--;
        while(undecidedCount!=0){
            Tile start = getUndecided();
            Tile current = start;

            while(!decided[current.column][current.row]) {
                next[current.column][current.row] = randomAdjacent(current);
                current = next[current.column][current.row];
            }
            current = start;
            while(!decided[current.column][current.row]) {
                decided[current.column][current.row]=true;
                undecidedCount--;
                current = next[current.column][current.row];
            }
        }

        for(int column = 0; column<columnCount; column++)
            for(int row = 0; row <rowCount; row++){
                vWall[column][row]=(random.nextDouble()>openings);
                hWall[column][row]=(random.nextDouble()>openings);
            }
        for(int column = 0; column<columnCount; column++)
            for(int row = 0; row <rowCount; row++){

                if(next[column][row].column==column-1)
                    vWall[column][row]=false;
                if(next[column][row].column==column+1)
                    vWall[column+1][row]=false;
                if(next[column][row].row==row-1)
                    hWall[column][row]=false;
                if(next[column][row].row==row+1)
                    hWall[column][row+1]=false;
            }

    }


    private Tile getUndecided() {
        //Retourne une case aléatoire parmie celles qui ne sont pas encore dans "decided"
        int index = random.nextInt(undecidedCount);
        int column = 0;
        int row = 0;
        int i = 0;
        while (decided[column][row]||i<index) {
            if(!decided[column][row])
                i++;
            if (column==columnCount-1){
                column=0;
                row++;
            }
            else column++;
        }
        return new Tile(column,row);
    }

    public class Tile{
        int column, row;

        public Tile(int column, int row) {
            this.column = column;
            this.row = row;
        }
    }
}
