package com.iae;

import com.iae.dao.DatabaseManager;
import com.iae.ui.UiTheme;
import javafx.geometry.Rectangle2D;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Screen;
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
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double sceneWidth = Math.min(1280, Math.max(1120, visualBounds.getWidth() - 80));
        double sceneHeight = Math.min(760, Math.max(680, visualBounds.getHeight() - 100));
        Scene scene = new Scene(loader.load(), sceneWidth, sceneHeight);
        UiTheme.apply(scene);
        primaryStage.setTitle("IAE - Integrated Assignment Environment");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(650);
        primaryStage.show();
        primaryStage.centerOnScreen();
        primaryStage.setOnCloseRequest(event -> {
            try {
                DatabaseManager.getInstance().disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
