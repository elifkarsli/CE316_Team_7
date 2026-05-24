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
    @FXML private TextArea compileLogArea;
    @FXML private TextArea runOutputArea;
    @FXML private TextArea expectedOutputArea;
    @FXML private TextArea errorDetailsArea;

    public void setResult(StudentResult result) {
        setResult(result, null);
    }

    public void setResult(StudentResult result, String expectedOutput) {
        if (result == null) {
            studentLabel.setText("Student:");
            setAreaText(compileLogArea, "");
            setAreaText(runOutputArea, "");
            setAreaText(expectedOutputArea, expectedOutput);
            setAreaText(errorDetailsArea, "");
            return;
        }

        studentLabel.setText("Student: " + result.getStudentId());
        setAreaText(compileLogArea, result.getCompileLog());
        setAreaText(runOutputArea, result.getRunOutput());
        setAreaText(expectedOutputArea, expectedOutput);
        setAreaText(errorDetailsArea, result.getErrorDetails());
    }

    @FXML
    private void handleClose(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void setAreaText(TextArea area, String value) {
        if (area != null) {
            area.setText(value == null ? "" : value);
        }
    }
}
