package com.example.goldfinder;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class AppClient extends javafx.application.Application {
    private static final String VIEW_RESOURCE_PATH = "/com/example/goldfinder/gridView.fxml";
    private static final String APP_NAME = "Gold Finder";


    private Stage primaryStage;
    private Parent view;
    private Controller controller;
    private InformationsWindow informationsWindow;
    private Client client;
    @Override
    public void start(Stage primaryStage) throws IOException {
        initializePrimaryStage(primaryStage);
        initializeView();
        initializeInformationWindow();
    }

    private void initializePrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle(APP_NAME);
        this.primaryStage.setOnCloseRequest(event -> {
            try {
                client.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Platform.exit();
            System.exit(0);
        });
        this.primaryStage.setResizable(false);
        this.primaryStage.sizeToScene();
    }

    private void initializeView() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        URL location = AppClient.class.getResource(VIEW_RESOURCE_PATH);
        loader.setLocation(location);
        view = loader.load();
        controller = loader.getController();
        controller.initialize();
    }

    private void initializeInformationWindow(){
        try {
            informationsWindow = new InformationsWindow(this);
            informationsWindow.start(new Stage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public void finishStart(){
        showScene();
        Thread thread = new Thread(() -> initializeClient(getName()));
        thread.start();
    }

    private void showScene() {
        Scene scene = new Scene(view);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private String getName(){
        return informationsWindow.getPlayerName();
    }

    private void initializeClient(String playerName){
        client = new Client(playerName, controller, this);
        client.connect(informationsWindow.getIP(), informationsWindow.getPort());
    }

    public void setKeyPressed(){ view.setOnKeyPressed(controller::handleMove);}
    public void ending(String endingString){
        // Affichage du Board
        EndingWindow endingWindow = new EndingWindow(endingString);
        try {
            endingWindow.start(new Stage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static void main(String[] args) {
        launch(args);
    }
    public void echoMessage(String message){ System.out.println(message); }
}