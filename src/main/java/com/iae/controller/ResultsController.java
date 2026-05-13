package com.iae.controller;

import com.iae.model.Project;
import com.iae.model.StudentResult;
import com.iae.service.ProjectService;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;

public class ResultsController {

    // ---- FXML fields (wired from results.fxml) -------------------
    @FXML private Label                      projectTitleLabel;
    @FXML private Label                      lastRunLabel;
    @FXML private Label                      summaryLabel;
    @FXML private Button                     runButton;
    @FXML private Button                     viewDetailsButton;
    @FXML private TableView<StudentResult>   resultTable;

    @FXML private TableColumn<StudentResult, String> colStudentId;
    @FXML private TableColumn<StudentResult, String> colCompileStatus;
    @FXML private TableColumn<StudentResult, String> colRunStatus;
    @FXML private TableColumn<StudentResult, String> colComparison;
    @FXML private TableColumn<StudentResult, String> colOverall;

    private Project           project;
    private MainController    mainController;
    private final ProjectService projectService = new ProjectService();
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setProject(Project project) {
        this.project = project;
        projectTitleLabel.setText("Results – " + project.getName());
        String lastRun = project.getLastRunDate() != null
                ? "Last run: " + project.getLastRunDate()
                : "Not yet evaluated";
        lastRunLabel.setText(lastRun);
        loadResults();
    }

    @FXML
    private void initialize() {
        colStudentId.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getStudentId()));

        colCompileStatus.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getCompileStatus()));

        colRunStatus.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getRunStatus()));

        colComparison.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getComparisonResult()));

        colOverall.setCellValueFactory(data -> {
            String cmp = data.getValue().getComparisonResult();
            return new SimpleStringProperty("PASS".equals(cmp) ? "✔ PASS" : "✘ FAIL");
        });

        resultTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(StudentResult item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if ("PASS".equals(item.getComparisonResult())) {
                    setStyle("-fx-background-color: #E8F5E9;");
                } else if ("COMPILE_ERROR".equals(item.getCompileStatus())) {
                    setStyle("-fx-background-color: #FFEBEE;");
                } else if ("RUNTIME_ERROR".equals(item.getRunStatus())) {
                    setStyle("-fx-background-color: #FFF3E0;");
                } else {
                    setStyle("-fx-background-color: #FFF9C4;");
                }
            }
        });

        viewDetailsButton.disableProperty().bind(
                resultTable.getSelectionModel().selectedItemProperty().isNull());
    }

    private void loadResults() {
        if (project == null) return;
        try {
            List<StudentResult> results = projectService.getResults(project.getId());
            resultTable.getItems().setAll(results);
            updateSummary(results);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Load Error",
                    "Could not load results: " + e.getMessage());
        }
    }

    @FXML
    void handleRun() {
        if (project == null) return;

        runButton.setDisable(true);
        runButton.setText("Running…");
        if (mainController != null) mainController.setStatus("Evaluating submissions…");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // TODO Tina: OutputComparator and ReportService are called
                //  Uncomment the line below
                // projectService.runProject(project);

                projectService.runProject(project);
                return null;
            }

            @Override
            protected void succeeded() {
                runButton.setDisable(false);
                runButton.setText("▶  Run Evaluation");
                loadResults();
                lastRunLabel.setText("Last run: " + project.getLastRunDate());
                if (mainController != null)
                    mainController.setStatus("Evaluation complete – " + project.getName());
            }

            @Override
            protected void failed() {
                runButton.setDisable(false);
                runButton.setText("▶  Run Evaluation");
                if (mainController != null)
                    mainController.setStatus("Evaluation failed.");
                showAlert(Alert.AlertType.ERROR, "Run Error",
                        "Evaluation failed: " + getException().getMessage());
            }
        };

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    void handleViewDetails() {
        StudentResult selected = resultTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/result_detail.fxml"));
            Parent view = loader.load();

            // TODO Tina: ResultDetailController.setStudentResult()
            // Uncomment once result_detail.fxml and ResultDetailController are ready:

            // ResultDetailController rdc = loader.getController();
            // rdc.setStudentResult(selected);

            Stage detailStage = new Stage();
            detailStage.setTitle("Result Detail – " + selected.getStudentId());
            detailStage.setScene(new Scene(view, 700, 550));
            detailStage.show();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Detail Error",
                    "Could not open detail view: " + e.getMessage());
        }
    }

    private void updateSummary(List<StudentResult> results) {
        long passCount = results.stream()
                .filter(r -> "PASS".equals(r.getComparisonResult()))
                .count();
        summaryLabel.setText(
                results.size() + " submissions  |  "
                        + passCount + " passed  |  "
                        + (results.size() - passCount) + " failed");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.showAndWait();
    }
}