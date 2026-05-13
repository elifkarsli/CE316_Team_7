package com.iae.controller;

import com.iae.model.Configuration;
import com.iae.service.ConfigurationService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

public class ConfigurationController {
    private final ConfigurationService configurationService = new ConfigurationService();
    private final ObservableList<Configuration> configurations = FXCollections.observableArrayList();

    @FXML
    private TableView<Configuration> configurationTable;

    @FXML
    private TableColumn<Configuration, Number> idColumn;

    @FXML
    private TableColumn<Configuration, String> nameColumn;

    @FXML
    private TableColumn<Configuration, String> sourceFileColumn;

    @FXML
    private TableColumn<Configuration, String> compileCommandColumn;

    @FXML
    private TableColumn<Configuration, String> runCommandColumn;

    @FXML
    private TableColumn<Configuration, String> typeColumn;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button exportButton;

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getId()));
        nameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(valueOrEmpty(cellData.getValue().getName())));
        sourceFileColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(valueOrEmpty(cellData.getValue().getSourceFileName())));
        compileCommandColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(valueOrEmpty(cellData.getValue().getCompileCommand())));
        runCommandColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(valueOrEmpty(cellData.getValue().getRunCommand())));
        typeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isInterpreted() ? "Interpreted" : "Compiled"));

        configurationTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        configurationTable.setItems(configurations);

        editButton.disableProperty().bind(Bindings.isNull(configurationTable.getSelectionModel().selectedItemProperty()));
        deleteButton.disableProperty().bind(Bindings.isNull(configurationTable.getSelectionModel().selectedItemProperty()));
        exportButton.disableProperty().bind(Bindings.isNull(configurationTable.getSelectionModel().selectedItemProperty()));

        refreshConfigurations();
    }

    @FXML
    private void handleNew() {
        openEditDialog(null);
    }

    @FXML
    private void handleEdit() {
        Configuration selectedConfiguration = getSelectedConfiguration();
        if (selectedConfiguration != null) {
            openEditDialog(selectedConfiguration);
        }
    }

    @FXML
    private void handleDelete() {
        Configuration selectedConfiguration = getSelectedConfiguration();
        if (selectedConfiguration == null) {
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Configuration");
        confirmation.setHeaderText("Delete selected configuration?");
        confirmation.setContentText(selectedConfiguration.getName());

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            boolean deleted = configurationService.deleteConfiguration(selectedConfiguration.getId());
            if (deleted) {
                refreshConfigurations();
                showInformation("Configuration deleted successfully.");
            } else {
                showError("Configuration could not be found.");
            }
        } catch (IOException exception) {
            showError("Configuration could not be deleted.", exception);
        }
    }

    @FXML
    private void handleImport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Configurations");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON files", "*.json"));

        File selectedFile = fileChooser.showOpenDialog(getWindow());
        if (selectedFile == null) {
            return;
        }

        try {
            List<Configuration> importedConfigurations =
                    configurationService.importConfigurations(selectedFile.toPath());
            refreshConfigurations();
            showInformation(importedConfigurations.size() + " configuration(s) imported successfully.");
        } catch (IOException | IllegalArgumentException exception) {
            showError("Configurations could not be imported.", exception);
        }
    }

    @FXML
    private void handleExport() {
        Configuration selectedConfiguration = getSelectedConfiguration();
        if (selectedConfiguration == null) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Configuration");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON files", "*.json"));
        fileChooser.setInitialFileName(safeFileName(selectedConfiguration.getName()) + ".json");

        File selectedFile = fileChooser.showSaveDialog(getWindow());
        if (selectedFile == null) {
            return;
        }

        try {
            configurationService.exportConfiguration(selectedConfiguration.getId(), selectedFile.toPath());
            showInformation("Configuration exported successfully.");
        } catch (IOException | IllegalArgumentException exception) {
            showError("Configuration could not be exported.", exception);
        }
    }

    @FXML
    private void handleHelp() {
        URL helpViewUrl = getClass().getResource("/fxml/help.fxml");
        if (helpViewUrl == null) {
            showError("Help screen could not be found.");
            return;
        }

        try {
            Parent root = FXMLLoader.load(helpViewUrl);

            Stage dialog = new Stage();
            dialog.setTitle("IAE Help");
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(getWindow());
            dialog.setScene(new Scene(root));
            dialog.show();
        } catch (IOException exception) {
            showError("Help screen could not be opened.", exception);
        }
    }

    private void refreshConfigurations() {
        try {
            configurations.setAll(configurationService.listConfigurations());
        } catch (IOException exception) {
            showError("Configurations could not be loaded.", exception);
        }
    }

    private void openEditDialog(Configuration configuration) {
        URL editViewUrl = getClass().getResource("/fxml/config_edit.fxml");
        if (editViewUrl == null) {
            showInformation("Configuration edit form is not ready yet.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(editViewUrl);
            Parent root = loader.load();
            ConfigEditController controller = loader.getController();
            controller.setConfiguration(configuration);

            Stage dialog = new Stage();
            dialog.setTitle(configuration == null ? "New Configuration" : "Edit Configuration");
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(getWindow());
            dialog.setScene(new Scene(root));
            controller.setDialogStage(dialog);
            dialog.showAndWait();

            if (controller.isSaved()) {
                refreshConfigurations();
            }
        } catch (IOException exception) {
            showError("Configuration edit form could not be opened.", exception);
        }
    }

    private Configuration getSelectedConfiguration() {
        return configurationTable.getSelectionModel().getSelectedItem();
    }

    private Window getWindow() {
        if (configurationTable == null || configurationTable.getScene() == null) {
            return null;
        }
        return configurationTable.getScene().getWindow();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String safeFileName(String value) {
        String fileName = valueOrEmpty(value).trim().replaceAll("[^a-zA-Z0-9._-]+", "_");
        return fileName.isEmpty() ? "configuration" : fileName;
    }

    private void showInformation(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Configuration");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        showError(message, null);
    }

    private void showError(String message, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Configuration Error");
        alert.setHeaderText(message);
        alert.setContentText(exception == null ? null : exception.getMessage());
        alert.showAndWait();
    }
}
