package com.iae.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.iae.model.Project;
import com.iae.service.ProjectService;
import com.iae.ui.UiTheme;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainController {

    @FXML private javafx.scene.layout.StackPane contentArea;
    @FXML private VBox projectListContainer;
    @FXML private ScrollPane projectScrollPane;
    @FXML private Label statusBar;
    @FXML private Label welcomeLabel;
    @FXML private VBox welcomePane;
    @FXML private MenuBar menuBar;

    private final ProjectService projectService = new ProjectService();
    private final List<ProjectService.SavedProjectInfo> savedProjects = new ArrayList<>();
    private Path selectedProjectPath;

    @FXML
    private void initialize() {
        if (welcomePane != null) {
            welcomePane.setMaxHeight(VBox.USE_PREF_SIZE);
            welcomePane.setMinHeight(VBox.USE_PREF_SIZE);
            welcomePane.setMaxWidth(820);
        }
        if (menuBar != null) {
            menuBar.setMaxWidth(Region.USE_PREF_SIZE);
        }
        statusBar.setText("Ready");
        loadSavedProjects();
    }

    private void loadSavedProjects() {
        savedProjects.clear();
        try {
            savedProjects.addAll(projectService.listSavedProjects());
            renderSavedProjects();
        } catch (Exception e) {
            statusBar.setText("Could not load saved projects: " + e.getMessage());
        }
    }

    public void refreshSavedProjects() {
        loadSavedProjects();
    }

    private void openSavedProject(Path dbFile) {
        try {
            openProjectFile(dbFile);
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
            statusBar.setText("Creating a new project...");
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
            openProjectFile(file.toPath());
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
            statusBar.setText("Managing configurations...");
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
            Scene scene = new Scene(view, 900, 650);
            UiTheme.apply(scene);
            helpStage.setScene(scene);
            helpStage.setTitle("IAE - User Manual");
            helpStage.show();
        } catch (Exception e) {
            showError("Help Error", "Could not open the user manual: " + e.getMessage());
        }
    }

    @FXML
    void handleExit() {
        Platform.exit();
    }

    @FXML
    void handleExitClick(MouseEvent event) {
        handleExit();
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
        if (welcomePane != null) {
            contentArea.getChildren().add(welcomePane);
            javafx.scene.layout.StackPane.setAlignment(
                    welcomePane, javafx.geometry.Pos.TOP_CENTER);
            javafx.scene.layout.StackPane.setMargin(
                    welcomePane, new javafx.geometry.Insets(14, 0, 0, 0));
        } else {
            contentArea.getChildren().add(welcomeLabel);
        }
        statusBar.setText("Ready");
    }

    public void setStatus(String message) {
        statusBar.setText(message);
    }

    public void addToSidebar(ProjectService.SavedProjectInfo projectInfo) {
        boolean alreadyPresent = savedProjects.stream()
                .anyMatch(existing -> existing.dbFile().toAbsolutePath().normalize()
                        .equals(projectInfo.dbFile().toAbsolutePath().normalize()));
        if (!alreadyPresent) {
            savedProjects.add(projectInfo);
            renderSavedProjects();
        }
    }

    private void openProjectFile(Path dbFile) throws Exception {
        Project project = projectService.openProject(dbFile);
        ProjectService.SavedProjectInfo info = new ProjectService.SavedProjectInfo(project.getName(), dbFile);
        addToSidebar(info);
        selectedProjectPath = normalizePath(dbFile);
        updateSidebarSelection();
        loadResultsView(project);
        statusBar.setText("Opened: " + project.getName());
    }

    private void renderSavedProjects() {
        if (projectListContainer == null) {
            return;
        }

        projectListContainer.getChildren().clear();
        if (savedProjects.isEmpty()) {
            Label empty = new Label("No saved projects yet.");
            empty.getStyleClass().add("sidebar-empty");
            empty.setWrapText(true);
            projectListContainer.getChildren().add(empty);
            return;
        }

        for (ProjectService.SavedProjectInfo info : savedProjects) {
            projectListContainer.getChildren().add(createProjectCard(info));
        }
        updateSidebarSelection();
    }

    private VBox createProjectCard(ProjectService.SavedProjectInfo info) {
        VBox card = new VBox(4);
        card.getStyleClass().add("project-card");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinHeight(74);
        card.setPrefHeight(74);
        card.setCursor(Cursor.HAND);
        card.setUserData(normalizePath(info.dbFile()));

        Label title = new Label(info.name());
        title.getStyleClass().add("project-card-title");
        title.setWrapText(true);

        Label subtitle = new Label(info.dbFile().getFileName().toString());
        subtitle.getStyleClass().add("project-card-subtitle");
        subtitle.setWrapText(true);

        card.getChildren().addAll(title, subtitle);
        card.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            selectedProjectPath = normalizePath(info.dbFile());
            updateSidebarSelection();
            if (event.getClickCount() == 2) {
                openSavedProject(info.dbFile());
            }
        });
        return card;
    }

    private void updateSidebarSelection() {
        if (projectListContainer == null) {
            return;
        }

        for (Node node : projectListContainer.getChildren()) {
            if (!(node instanceof VBox card)) {
                continue;
            }
            boolean selected = selectedProjectPath != null
                    && selectedProjectPath.equals(card.getUserData());
            card.getStyleClass().remove("selected");
            if (selected) {
                card.getStyleClass().add("selected");
            }
        }
    }

    private Path normalizePath(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize();
    }

    private void swapContent(Parent view) {
        contentArea.getChildren().setAll(view);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(message);
        UiTheme.styleAlert(alert);
        alert.showAndWait();
    }
}
