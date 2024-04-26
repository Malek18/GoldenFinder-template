package com.example.goldfinder;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class InformationsWindow extends Application {

    private AppClient appClient;
    private Controller controller;
    private Stage primaryStage;
    private FlowPane flowPane;
    private Label name, ip, port;
    private TextField textFieldName, textFieldIP, textFieldPort;
    private Button submit;
    private HBox hbox, hboxName, hboxIP, hboxPort;
    private GridPane gridPane;
    private boolean information = false;

    public InformationsWindow(AppClient appClient){
        this.appClient = appClient;
    }
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("GetNameWindow");
        initialize();
        setOnAction();
        primaryStage.setScene(new Scene(flowPane));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void initialize(){
        flowPane = new FlowPane();

        submit = new Button("Submit & connect");

        name = new Label("Enter your username : ");
        textFieldName = new TextField();

        ip = new Label("Enter the server ip : ");
        textFieldIP = new TextField();

        port = new Label("Enter the server port : ");
        textFieldPort = new TextField();

        hbox = new HBox();
        gridPane = new GridPane();

        /*hboxName = new HBox();
        hboxIP = new HBox();
        hboxPort = new HBox();*/

        gridPane.add(name, 0, 0);
        gridPane.add(textFieldName, 1, 0);
        gridPane.add(ip, 0, 1);
        gridPane.add(textFieldIP, 1, 1);
        gridPane.add(port, 0, 2);
        gridPane.add(textFieldPort, 1, 2);

        gridPane.setHgap(10);
        gridPane.setVgap(10);

        hbox.getChildren().addAll(gridPane, submit);
        hbox.setSpacing(10);
        hbox.setAlignment(Pos.CENTER);

        flowPane.getChildren().add(hbox);
        FlowPane.setMargin(hbox, new Insets(20));
    }

    private void setOnAction(){
        submit.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (!textFieldName.getText().isEmpty()){
                    primaryStage.close();
                    try {
                        appClient.finishStart();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    public String getPlayerName(){ return this.textFieldName.getText(); }
    public String getIP(){ return this.textFieldIP.getText(); }
    public int getPort(){ return Integer.parseInt(this.textFieldPort.getText()); }
    public boolean getInformation(){ return this.information; }
}
