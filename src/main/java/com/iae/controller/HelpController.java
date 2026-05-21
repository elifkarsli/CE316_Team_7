package com.iae.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.net.URL;

public class HelpController {
    private static final String MANUAL_RESOURCE_PATH = "/help/manual.html";

    @FXML
    private WebView manualWebView;

    @FXML
    private void initialize() {
        loadManual();
    }

    private void loadManual() {
        if (manualWebView == null) {
            showError("Manual viewer could not be initialized.");
            return;
        }

        URL manualUrl = getClass().getResource(MANUAL_RESOURCE_PATH);
        if (manualUrl == null) {
            showError("Manual file could not be found.");
            return;
        }

        WebEngine webEngine = manualWebView.getEngine();
        webEngine.load(manualUrl.toExternalForm());
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Help Error");
        alert.setHeaderText(message);
        alert.showAndWait();
    }
}
