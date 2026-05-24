package com.iae.controller;

import java.util.List;

import com.iae.model.Project;
import com.iae.model.StudentResult;
import com.iae.service.ProjectService;
import com.iae.service.ReportService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ResultsController {
    @FXML private Label projectTitleLabel;
    @FXML private Label lastRunLabel;
    @FXML private Label summaryLabel;
    @FXML private Button runButton;
    @FXML private Button viewDetailsButton;
    @FXML private TableView<StudentResult> resultTable;
    @FXML private TableColumn<StudentResult, String> colStudentId;
    @FXML private TableColumn<StudentResult, String> colCompileStatus;
    @FXML private TableColumn<StudentResult, String> colRunStatus;
    @FXML private TableColumn<StudentResult, String> colComparison;
    @FXML private TableColumn<StudentResult, String> colOverall;
    @FXML private TableColumn<StudentResult, Void> detailCol;

    private Project project;
    private MainController mainController;
    private final ProjectService projectService = new ProjectService();
    private final ReportService reportService = new ReportService();

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setProject(Project project) {
        this.project = project;
        projectTitleLabel.setText("Project: " + project.getName());
        lastRunLabel.setText(project.getLastRunDate() != null
                ? "Last run: " + project.getLastRunDate()
                : "Not yet evaluated");
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
            String comparison = data.getValue().getComparisonResult();
            return new SimpleStringProperty("PASS".equals(comparison) ? "PASS" : "FAIL");
        });

        detailCol.setSortable(false);
        detailCol.setCellFactory(column -> new TableCell<>() {
            private final Button detailsButton = new Button("Details");

            {
                detailsButton.setOnAction(event -> {
                    StudentResult result = getTableView().getItems().get(getIndex());
                    if (result != null) {
                        openDetailView(result);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : detailsButton);
            }
        });

        resultTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(StudentResult item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                    return;
                }

                if ("PASS".equals(item.getComparisonResult())) {
                    setStyle("-fx-background-color: #D4EDDA;");
                } else if ("COMPILE_ERROR".equals(item.getCompileStatus())
                        || "RUNTIME_ERROR".equals(item.getRunStatus())
                        || "FAIL".equals(item.getComparisonResult())) {
                    setStyle("-fx-background-color: #F8D7DA;");
                } else {
                    setStyle("");
                }
            }
        });

        viewDetailsButton.disableProperty().bind(
                resultTable.getSelectionModel().selectedItemProperty().isNull());
    }

    private void loadResults() {
        if (project == null) {
            return;
        }

        try {
            List<StudentResult> results = reportService.getResultsByProject(project.getId());
            resultTable.setItems(FXCollections.observableArrayList(results));
            updateSummary(results);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Load Error",
                    "Could not load results: " + e.getMessage());
        }
    }

    @FXML
    void handleRun() {
        if (project == null) {
            return;
        }

        runButton.setDisable(true);
        runButton.setText("Running...");
        if (mainController != null) {
            mainController.setStatus("Evaluating submissions...");
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                projectService.runProject(project);
                return null;
            }

            @Override
            protected void succeeded() {
                runButton.setDisable(false);
                runButton.setText("Run Project");
                loadResults();
                lastRunLabel.setText("Last run: " + project.getLastRunDate());
                if (mainController != null) {
                    mainController.setStatus("Evaluation complete - " + project.getName());
                }
            }

            @Override
            protected void failed() {
                runButton.setDisable(false);
                runButton.setText("Run Project");
                if (mainController != null) {
                    mainController.setStatus("Evaluation failed.");
                }
                Throwable exception = getException();
                String message = exception == null ? "Unknown error." :
                        (exception.getMessage() != null ? exception.getMessage() : exception.toString());
                showAlert(Alert.AlertType.ERROR, "Run Error",
                        "Evaluation failed: " + message);
            }
        };

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    void handleViewDetails() {
        StudentResult selected = resultTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openDetailView(selected);
        }
    }

    private void openDetailView(StudentResult selected) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/result_detail.fxml"));
            Parent view = loader.load();

            ResultDetailController controller = loader.getController();
            controller.setResult(selected);

            Stage detailStage = new Stage();
            detailStage.initModality(Modality.APPLICATION_MODAL);
            if (resultTable.getScene() != null) {
                detailStage.initOwner(resultTable.getScene().getWindow());
            }
            detailStage.setTitle("Result Detail - " + selected.getStudentId());
            detailStage.setScene(new Scene(view, 700, 550));
            detailStage.showAndWait();
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
