package com.iae;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/configuration.fxml"));

        stage.setTitle("IAE");
        stage.setScene(new Scene(root));
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }
}
