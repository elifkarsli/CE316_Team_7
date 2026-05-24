package com.iae.ui;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;

public final class UiTheme {

    private static final String STYLESHEET = "/css/iae-theme.css";
    private static final Color BACKGROUND = Color.web("#08100e");
    private static final String TABLE_STYLE = String.join("; ",
            "-fx-base: #0f1513",
            "-fx-control-inner-background: #0f1513",
            "-fx-control-inner-background-alt: #111916",
            "-fx-background-color: #0f1513",
            "-fx-table-cell-border-color: rgba(255, 255, 255, 0.06)",
            "-fx-selection-bar: #244639",
            "-fx-selection-bar-non-focused: #1d332a",
            "-fx-focus-color: #4a8e73",
            "-fx-faint-focus-color: transparent");

    private UiTheme() {
    }

    public static void apply(Scene scene) {
        if (scene == null) {
            return;
        }

        String stylesheet = UiTheme.class.getResource(STYLESHEET).toExternalForm();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
        scene.setFill(BACKGROUND);
    }

    public static void styleDataTable(TableView<?> tableView) {
        if (tableView == null) {
            return;
        }

        if (!tableView.getStyleClass().contains("data-table")) {
            tableView.getStyleClass().add("data-table");
        }
        tableView.setStyle(TABLE_STYLE);
    }

    public static void styleAlert(Alert alert) {
        if (alert == null) {
            return;
        }

        DialogPane dialogPane = alert.getDialogPane();
        if (!dialogPane.getStylesheets().contains(getStylesheet())) {
            dialogPane.getStylesheets().add(getStylesheet());
        }
        if (!dialogPane.getStyleClass().contains("iae-dialog-pane")) {
            dialogPane.getStyleClass().add("iae-dialog-pane");
        }
        dialogPane.setStyle("-fx-background-color: #0f1513;");
        alert.setOnShown(event -> {
            Scene scene = dialogPane.getScene();
            if (scene != null) {
                scene.setFill(BACKGROUND);
            }
        });
    }

    private static String getStylesheet() {
        return UiTheme.class.getResource(STYLESHEET).toExternalForm();
    }
}
