package com.iae.controller;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import com.iae.model.Configuration;
import com.iae.model.Project;
import com.iae.service.ConfigurationService;
import com.iae.service.ProjectService;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

public class ProjectSetupController {

    @FXML private TextField              nameField;
    @FXML private ComboBox<Configuration> configCombo;
    @FXML private TextField              dirField;
    @FXML private Label                  messageLabel;

    private final ConfigurationService configService = new ConfigurationService();
    private final ProjectService projectService = new ProjectService();

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        populateConfigCombo();
    }

    private void populateConfigCombo() {
        try {
            List<Configuration> configs = configService.listConfigurations();
            configCombo.getItems().setAll(configs);
            if (!configs.isEmpty()) {
                configCombo.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            showMessage("Could not load configurations: " + e.getMessage());
        }
    }

    @FXML
    void handleBrowse() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Submissions Folder");
        File selected = dc.showDialog(dirField.getScene().getWindow());
        if (selected != null) {
            dirField.setText(selected.getAbsolutePath());
        }
    }

    @FXML
    void handleCreate() {
        String name = trim(nameField.getText());
        Configuration cfg = configCombo.getSelectionModel().getSelectedItem();
        String dir = trim(dirField.getText());

        if (name.isEmpty()) {
            showMessage("Project name is required.");
            return;
        }
        if (cfg == null) {
            showMessage("Please select a configuration.");
            return;
        }
        if (dir.isEmpty()) {
            showMessage("Please select a submissions folder.");
            return;
        }

        try {
            Project project = projectService.createProject(name, cfg.getId(), Path.of(dir));

            // Register the new project in the sidebar and navigate to results
            if (mainController != null) {
                mainController.refreshSavedProjects();
                mainController.loadResultsView(project);
                mainController.setStatus("Project created: " + project.getName());
            }
        } catch (Exception e) {
            showMessage("Could not create project: " + e.getMessage());
        }
    }

    @FXML
    void handleCancel() {
        if (mainController != null) {
            mainController.showWelcome();
        }
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void hideMessage() {
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }
}