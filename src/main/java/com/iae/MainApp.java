package com.iae;

import com.iae.dao.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;

public class MainApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Path dataDir = Path.of("data");
        Files.createDirectories(dataDir);
        Path mainDb = dataDir.resolve("iae_app.iaedb");
        DatabaseManager.getInstance().connect(mainDb.toAbsolutePath().toString());

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Scene scene = new Scene(loader.load(), 1280, 840);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        primaryStage.setTitle("IAE – Integrated Assignment Environment");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(720);
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> {
            try {
                DatabaseManager.getInstance().disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
