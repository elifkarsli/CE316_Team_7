package com.iae.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.net.URL;

public class HelpController {
    @FXML
    private WebView manualWebView;

    @FXML
    private void initialize() {
        URL manualUrl = getClass().getResource("/help/manual.html");
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
