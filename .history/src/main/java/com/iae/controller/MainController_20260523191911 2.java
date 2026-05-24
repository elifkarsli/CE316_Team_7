package com.iae.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.iae.model.Project;
import com.iae.service.ProjectService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainController {

    @FXML private StackPane    contentArea;
    @FXML private ListView<ProjectService.SavedProjectInfo> projectListView;
    @FXML private Label        statusBar;
    @FXML private Label        welcomeLabel;
    @FXML private MenuBar      menuBar;

    private final ProjectService projectService = new ProjectService();

    @FXML
    private void initialize() {
        statusBar.setText("Ready");
        projectListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ProjectService.SavedProjectInfo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatDisplayLabel(item));
            }
        });

        projectListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ProjectService.SavedProjectInfo selected = projectListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openSavedProject(selected.dbFile());
                }
            }
        });
        loadSavedProjects();
    }

    private void loadSavedProjects() {
        projectListView.getItems().clear();
        try {
            for (ProjectService.SavedProjectInfo info : projectService.listSavedProjects()) {
                addToSidebar(info);
            }
        } catch (Exception e) {
            statusBar.setText("Could not load saved projects: " + e.getMessage());
        }
    }

    public void refreshSavedProjects() {
        loadSavedProjects();
    }

    private void openSavedProject(Path dbFile) {
        try {
            Project project = projectService.openProject(dbFile);
            ProjectService.SavedProjectInfo info = new ProjectService.SavedProjectInfo(project.getName(), dbFile);
            addToSidebar(info);
            loadResultsView(project);
            statusBar.setText("Opened: " + project.getName());
        } catch (Exception e) {
            showError("Open Error", "Could not open saved project: " + e.getMessage());
        }
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
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("IAE Project Files", "*.iaedb", "*.db"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        Path dataDir = Paths.get("data");
        if (Files.isDirectory(dataDir)) {
            fc.setInitialDirectory(dataDir.toFile());
        }

        File file = fc.showOpenDialog(contentArea.getScene().getWindow());
        if (file == null) return;

        try {
            Project project = projectService.openProject(file.toPath());
            ProjectService.SavedProjectInfo info = new ProjectService.SavedProjectInfo(project.getName(), file.toPath());
            addToSidebar(info);
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

    public void addToSidebar(ProjectService.SavedProjectInfo projectInfo) {
        boolean alreadyPresent = projectListView.getItems().stream()
                .anyMatch(existing -> existing.dbFile().toAbsolutePath().normalize()
                        .equals(projectInfo.dbFile().toAbsolutePath().normalize()));
        if (!alreadyPresent) {
            projectListView.getItems().add(projectInfo);
        }
    }

    private String formatDisplayLabel(ProjectService.SavedProjectInfo info) {
        return String.format("%s - %s", info.name(), info.dbFile().getFileName());
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
