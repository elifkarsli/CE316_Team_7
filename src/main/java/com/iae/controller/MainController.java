package com.iae.controller;

import com.iae.model.Project;
import com.iae.service.ProjectService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.sql.SQLException;

public class MainController {

    @FXML private StackPane    contentArea;
    @FXML private ListView<String> projectListView;
    @FXML private Label        statusBar;
    @FXML private Label        welcomeLabel;
    @FXML private MenuBar      menuBar;

    private final ProjectService projectService = new ProjectService();

    @FXML
    private void initialize() {
        statusBar.setText("Ready");

        projectListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = projectListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    statusBar.setText("Double-click to re-open projects via File > Open Project.");
                }
            }
        });

        Platform.runLater(this::wireExitMenuClick);
    }

    @FXML
    void handleNewProject() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/project_setup.fxml"));
            Parent view = loader.load();


            ProjectSetupController psc = loader.getController();
            psc.setMainController(this);

            swapContent(view);
            statusBar.setText("Creating a new project…");
        } catch (Exception e) {
            showError("Navigation Error",
                    "Could not load the project setup screen: " + e.getMessage());
        }
    }


    @FXML
    void handleOpenProject() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open IAE Project");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("IAE Project Files", "*.iaedb"));

        File file = fc.showOpenDialog(contentArea.getScene().getWindow());
        if (file == null) return;

        try {
            Project project = projectService.openProject(file.toPath());
            addToSidebar(project.getName());
            loadResultsView(project);
            statusBar.setText("Opened: " + project.getName());
        } catch (Exception e) {
            showError("Open Error", "Could not open project: " + e.getMessage());
        }
    }


    @FXML
    void handleManageConfigs() {
        try {
            Parent view = FXMLLoader.load(
                    getClass().getResource("/fxml/configuration.fxml"));
            swapContent(view);
            statusBar.setText("Managing configurations…");
        } catch (Exception e) {
            showError("Navigation Error",
                    "Could not load configurations screen: " + e.getMessage());
        }
    }


    @FXML
    void handleHelp() {
        try {
            Stage helpStage = new Stage();
            Parent view = FXMLLoader.load(
                    getClass().getResource("/fxml/help.fxml"));
            helpStage.setScene(new Scene(view, 900, 650));
            helpStage.setTitle("IAE – User Manual");
            helpStage.show();
        } catch (Exception e) {
            showError("Help Error", "Could not open the user manual: " + e.getMessage());
        }
    }


    @FXML
    void handleExit() {
        Platform.exit();
        Runtime.getRuntime().halt(0);
    }

    private void wireExitMenuClick() {
        for (Node menuNode : menuBar.lookupAll(".menu")) {
            Node labelNode = menuNode.lookup(".label");
            if (labelNode instanceof Labeled label && "Exit".equals(label.getText())) {
                menuNode.setOnMouseClicked(event -> {
                    event.consume();
                    handleExit();
                });
                labelNode.setOnMouseClicked(event -> {
                    event.consume();
                    handleExit();
                });
                return;
            }
        }
    }


    public void loadResultsView(Project project) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/results.fxml"));
            Parent view = loader.load();

            ResultsController rc = loader.getController();
            rc.setMainController(this);
            rc.setProject(project);

            swapContent(view);
            statusBar.setText("Project: " + project.getName());
        } catch (Exception e) {
            showError("Navigation Error",
                    "Could not load the results screen: " + e.getMessage());
        }
    }


    public void showWelcome() {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(welcomeLabel);
        statusBar.setText("Ready");
    }


    public void setStatus(String message) {
        statusBar.setText(message);
    }


    public void addToSidebar(String projectName) {
        if (!projectListView.getItems().contains(projectName)) {
            projectListView.getItems().add(projectName);
        }
    }


    private void swapContent(Parent view) {
        contentArea.getChildren().setAll(view);
    }


    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.showAndWait();
    }
}
