package com.iae.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.iae.model.Configuration;
import com.iae.model.Project;
import com.iae.model.StudentResult;
import com.iae.service.ConfigurationService;
import com.iae.service.ProjectService;
import com.iae.service.ReportService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
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
    private final ConfigurationService configurationService = new ConfigurationService();
    private String selectedStudentId;

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
            private final StackPane wrapper = new StackPane(detailsButton);

            {
                detailsButton.getStyleClass().addAll("secondary-button", "compact-button");
                detailsButton.setOnAction(event -> {
                    StudentResult result = getTableView().getItems().get(getIndex());
                    if (result != null) {
                        openDetailView(result);
                    }
                });
                detailsButton.setMinWidth(78);
                detailsButton.setPrefWidth(78);
                detailsButton.setMaxWidth(Double.MAX_VALUE);
                wrapper.setPadding(new Insets(4, 0, 4, 0));
                wrapper.setMaxWidth(Double.MAX_VALUE);
                StackPane.setAlignment(detailsButton, Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : wrapper);
            }
        });

        resultTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(StudentResult item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("");
                if (item == null || empty) {
                    return;
                }
            }
        });
        resultTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedStudentId = newValue == null ? null : newValue.getStudentId();
        });
        resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        resultTable.setStyle(
                "-fx-base: #0f1513;"
                        + "-fx-control-inner-background: #0f1513;"
                        + "-fx-control-inner-background-alt: #111916;"
                        + "-fx-background-color: #0f1513;"
                        + "-fx-table-cell-border-color: rgba(255, 255, 255, 0.06);"
                        + "-fx-selection-bar: #244639;"
                        + "-fx-selection-bar-non-focused: #1d332a;"
                        + "-fx-focus-color: #4a8e73;"
                        + "-fx-faint-focus-color: transparent;");

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
            if (!results.isEmpty()) {
                StudentResult match = null;
                if (selectedStudentId != null) {
                    match = results.stream()
                            .filter(result -> selectedStudentId.equals(result.getStudentId()))
                            .findFirst()
                            .orElse(null);
                }
                if (match != null) {
                    resultTable.getSelectionModel().select(match);
                } else {
                    resultTable.getSelectionModel().selectFirst();
                }
            } else {
                resultTable.getSelectionModel().clearSelection();
            }
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
            controller.setResult(selected, loadExpectedOutputText());

            Stage detailStage = new Stage();
            detailStage.initModality(Modality.APPLICATION_MODAL);
            if (resultTable.getScene() != null) {
                detailStage.initOwner(resultTable.getScene().getWindow());
            }
            detailStage.setTitle("Result Detail - " + selected.getStudentId());
            Scene scene = new Scene(view, 920, 780);
            scene.getStylesheets().add(
                    getClass().getResource("/css/styles.css").toExternalForm());
            scene.setFill(Color.web("#08100e"));
            detailStage.setScene(scene);
            detailStage.showAndWait();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Detail Error",
                    "Could not open detail view: " + e.getMessage());
        }
    }

    private String loadExpectedOutputText() {
        if (project == null) {
            return "No project selected.";
        }

        try {
            Configuration configuration = configurationService
                    .findById(project.getConfigurationId())
                    .orElse(null);
            if (configuration == null) {
                return "Expected output configuration not found.";
            }

            String expectedOutputPathValue = configuration.getExpectedOutputPath();
            if (expectedOutputPathValue == null || expectedOutputPathValue.isBlank()) {
                return "No expected output configured for this project.";
            }

            Path expectedOutputPath = Paths.get(expectedOutputPathValue);
            if (!Files.isRegularFile(expectedOutputPath)) {
                return "Expected output file not found: " + expectedOutputPath;
            }

            return Files.readString(expectedOutputPath);
        } catch (Exception e) {
            return "Could not load expected output: " + e.getMessage();
        }
    }

    private void updateSummary(List<StudentResult> results) {
        long passCount = results.stream()
                .filter(r -> "PASS".equals(r.getComparisonResult()))
                .count();
        summaryLabel.setText(
                results.size() + " submissions | "
                        + passCount + " passed | "
                        + (results.size() - passCount) + " need attention");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.showAndWait();
    }
}
