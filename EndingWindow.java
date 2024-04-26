package com.example.goldfinder;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.io.FileWriter;

public class EndingWindow extends Application {
    private String endingString;
    private VBox vBox;
    private ScrollPane scrollPane;
    private FileWriter fw;


    public EndingWindow(String endingString){ this.endingString = endingString; }

    //Methode de demarrage de l'application
    @Override
    public void start(Stage primaryStage) throws IOException {
        primaryStage.setTitle("Game Ended");
        initialize();
        //fermeture de la fenetre
        primaryStage.setOnCloseRequest(event -> Platform.exit());
        // Definition de la scene principale avec le vBox
        primaryStage.setScene(new Scene(vBox));
        // Empecher le redimensionnement de la fenetre
        primaryStage.setResizable(false);
        //Affichage de la fenetre principale
        primaryStage.show();
    }

    //Methode qui initialise la fenetre de fin de jeu .
    private void initialize() throws IOException {
        vBox = new VBox();
        Label label = new Label("Scoreboard");
        initializeScrollPane();
        vBox.getChildren().addAll(label, scrollPane);
        vBox.setSpacing(10);
        vBox.setAlignment(Pos.CENTER);
    }

    private void initializeScrollPane() throws IOException {
        // Creation d'un vBox pour contenir les elements à afficher dans la ScrollPane
        VBox vbox = new VBox();
        ArrayList<String> playerList = new ArrayList<>();
        ArrayList<Integer> scoreList = new ArrayList<>();

        //Extraire les données de fin partie de la chaine endingString
        endingString = endingString.substring(9, endingString.length() - 3);
        String[] stringTab = endingString.split(" ");
        //Parcours de la chaine pour recuperer les noms de joueurs et leurs scores
        for (String playerScore : stringTab){
            String[] tab = playerScore.split(":");
            playerList.add(tab[0]);
            scoreList.add(Integer.parseInt(tab[1]));
        }

        int nbPlayers = playerList.size();
        //Parcours pour afficher les joueurs dans l'ordre décroissant de leurs scores
        for (int classement = 0; classement < nbPlayers; classement++){
            String playerName = "";
            int max = 0;
            int index = 0;
            // Recherche du joueur avec le score le plus élevé
            for (int i = 0; i < scoreList.size(); i++) {
                if(scoreList.get(i) >max){
                    max = scoreList.get(i);
                    index = i;
                    playerName = playerList.get(i);
                }
            }
            //Suppression du joueur de la liste
            playerList.remove(index);
            scoreList.remove(index);
            //Ajout d'un élément PlayerBox pour representer le joueur
            vbox.getChildren().add(new PlayerBox(classement + 1,playerName,max));
            try {
                // Ecriture le classement,playerName et le score dans le fichier score.txt
                fw.write(+classement+".\t"+playerName+".\t"+max+".\t");

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
        try {
            // Fermeture du fichier score.txt
            fw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        scrollPane = new ScrollPane(vbox);
        scrollPane.minWidth(200);
        scrollPane.minHeight(400);
    }
    //creation du fichier score.txt
     private void createNewFile(){
              try {

                  String filename= "score.txt";
                  fw= new FileWriter("com.example.goldfinder/"+filename,true);
            }
            catch (Exception e) {
                System.err.println(e);
            }
        }
    }
