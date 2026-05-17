package com.iae.controller;

import com.iae.model.StudentResult;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class ResultDetailController {
    @FXML private Label studentLabel;
    @FXML private TextArea detailArea;

    public void setResult(StudentResult result) {
        if (result == null) {
            studentLabel.setText("Student:");
            detailArea.clear();
            return;
        }

        studentLabel.setText("Student: " + result.getStudentId());

        StringBuilder sb = new StringBuilder();
        sb.append("--- COMPILE STATUS: ")
                .append(valueOrEmpty(result.getCompileStatus()))
                .append(" ---\n");
        sb.append(valueOrEmpty(result.getCompileLog())).append('\n');
        sb.append("--- RUN STATUS: ")
                .append(valueOrEmpty(result.getRunStatus()))
                .append(" ---\n");
        sb.append("Output:\n")
                .append(valueOrEmpty(result.getRunOutput()));
        sb.append("\n--- COMPARISON RESULT: ")
                .append(valueOrEmpty(result.getComparisonResult()))
                .append(" ---");

        if (result.getErrorDetails() != null && !result.getErrorDetails().isBlank()) {
            sb.append("\n\n--- DETAILS ---\n")
                    .append(result.getErrorDetails());
        }

        detailArea.setText(sb.toString());
    }

    @FXML
    private void handleClose(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
