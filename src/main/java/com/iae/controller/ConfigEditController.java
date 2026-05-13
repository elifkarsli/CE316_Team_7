package com.iae.controller;

import com.iae.model.Configuration;
import com.iae.service.ConfigurationService;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class ConfigEditController {
    private final ConfigurationService configurationService = new ConfigurationService();

    private Configuration configuration;
    private Stage dialogStage;
    private boolean saved;

    @FXML
    private Label titleLabel;

    @FXML
    private TextField nameField;

    @FXML
    private TextField sourceFileField;

    @FXML
    private TextField outputFileField;

    @FXML
    private TextField compileCommandField;

    @FXML
    private TextField runCommandField;

    @FXML
    private TextField argumentsField;

    @FXML
    private TextArea expectedOutputPathField;

    @FXML
    private CheckBox interpretedCheckBox;

    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
        compileCommandField.disableProperty().bind(interpretedCheckBox.selectedProperty());
        interpretedCheckBox.selectedProperty().addListener((observable, oldValue, selected) ->
                updateCompileCommandPrompt(selected));
        updateCompileCommandPrompt(interpretedCheckBox.isSelected());
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;

        if (configuration == null) {
            titleLabel.setText("New Configuration");
            return;
        }

        titleLabel.setText("Edit Configuration");
        nameField.setText(valueOrEmpty(configuration.getName()));
        sourceFileField.setText(valueOrEmpty(configuration.getSourceFileName()));
        outputFileField.setText(valueOrEmpty(configuration.getOutputFileName()));
        compileCommandField.setText(valueOrEmpty(configuration.getCompileCommand()));
        runCommandField.setText(valueOrEmpty(configuration.getRunCommand()));
        argumentsField.setText(valueOrEmpty(configuration.getArguments()));
        expectedOutputPathField.setText(valueOrEmpty(configuration.getExpectedOutputPath()));
        interpretedCheckBox.setSelected(configuration.isInterpreted());
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleSave() {
        hideMessage();

        Configuration formConfiguration = buildConfigurationFromForm();
        try {
            if (configuration == null) {
                configurationService.saveConfiguration(formConfiguration);
            } else {
                formConfiguration.setId(configuration.getId());
                configurationService.updateConfiguration(formConfiguration);
            }

            saved = true;
            closeDialog();
        } catch (IOException | IllegalArgumentException exception) {
            showMessage(exception.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private Configuration buildConfigurationFromForm() {
        Configuration formConfiguration = new Configuration();
        formConfiguration.setName(trimToNull(nameField.getText()));
        formConfiguration.setSourceFileName(trimToNull(sourceFileField.getText()));
        formConfiguration.setOutputFileName(trimToNull(outputFileField.getText()));
        formConfiguration.setCompileCommand(trimToNull(compileCommandField.getText()));
        formConfiguration.setRunCommand(trimToNull(runCommandField.getText()));
        formConfiguration.setArguments(trimToNull(argumentsField.getText()));
        formConfiguration.setExpectedOutputPath(trimToNull(expectedOutputPathField.getText()));
        formConfiguration.setInterpreted(interpretedCheckBox.isSelected());
        return formConfiguration;
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
            return;
        }

        Stage currentStage = (Stage) nameField.getScene().getWindow();
        currentStage.close();
    }

    private void showMessage(String message) {
        messageLabel.setText(message == null || message.isBlank() ? "Configuration could not be saved." : message);
        messageLabel.setManaged(true);
        messageLabel.setVisible(true);
    }

    private void hideMessage() {
        messageLabel.setText("");
        messageLabel.setManaged(false);
        messageLabel.setVisible(false);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private void updateCompileCommandPrompt(boolean interpreted) {
        compileCommandField.setPromptText(interpreted ? "Not required for interpreted languages" : "javac Main.java");
    }
}
