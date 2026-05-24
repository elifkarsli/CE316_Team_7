package com.iae.service;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.iae.dao.DatabaseManager;
import com.iae.dao.ProjectDAO;
import com.iae.model.Configuration;
import com.iae.model.ProcessResult;
import com.iae.model.Project;
import com.iae.model.StudentResult;

public class ProjectService {
    public static record SavedProjectInfo(String name, Path dbFile) {}

    private final ProjectDAO projectDAO = new ProjectDAO();
    private final ConfigurationService configService = new ConfigurationService();
    private final ReportService reportService = new ReportService();
    private final ZipHandler zipHandler = new ZipHandler();
    private final ExecutionEngine executionEngine = new ExecutionEngine();
    private final OutputComparator outputComparator = new OutputComparator();

    public List<SavedProjectInfo> listSavedProjects() throws IOException {
        Path dataDir = Paths.get("data");
        if (Files.notExists(dataDir)) {
            return List.of();
        }

        List<SavedProjectInfo> savedProjects = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataDir, entry -> {
            String name = entry.getFileName().toString().toLowerCase();
            return name.endsWith(".iaedb") || name.endsWith(".db");
        })) {
            for (Path dbFile : stream) {
                Path normalizedDbFile = normalizeDbPath(dbFile);
                Optional<Project> project = readProjectFromDatabase(normalizedDbFile);
                project.ifPresent(value -> savedProjects.add(new SavedProjectInfo(value.getName(), normalizedDbFile)));
            }
        }

        return savedProjects;
    }

    private Path normalizeDbPath(Path dbFile) {
        return dbFile.toAbsolutePath().normalize();
    }

    private Optional<Project> readProjectFromDatabase(Path dbFile) throws IOException {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath())) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM projects LIMIT 1");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Project project = new Project();
                    project.setId(rs.getInt("id"));
                    project.setName(rs.getString("name"));
                    project.setConfigurationId(rs.getInt("configurationId"));
                    project.setSubmissionsDirectory(rs.getString("submissionsDirectory"));
                    project.setCreatedDate(rs.getString("createdDate"));
                    project.setLastRunDate(rs.getString("lastRunDate"));
                    return Optional.of(project);
                }
            }
        } catch (SQLException e) {
            throw new IOException("Could not read saved project from " + dbFile, e);
        }
        return Optional.empty();
    }

    public Project createProject(String name, int configId, Path submissionsDir) throws Exception {
        Configuration config = configService.findById(configId)
                .orElseThrow(() -> new Exception("Configuration not found: " + configId));

        Path projectDbFile = createProjectDatabaseFile(name);
        safeReconnectDatabase(projectDbFile);

        Project p = new Project();
        p.setName(name);
        p.setConfigurationId(config.getId());
        p.setSubmissionsDirectory(submissionsDir.toString());
        p.setCreatedDate(LocalDate.now().toString());
        projectDAO.save(p);
        return p;
    }

    public Project openProject(Path dbFile) throws Exception {
        Path normalizedDbFile = normalizeDbPath(dbFile);
        safeReconnectDatabase(normalizedDbFile);
        List<Project> projects = projectDAO.findAll();
        if (projects.isEmpty()) throw new Exception("No project found in this file.");
        return projects.get(0);
    }

    public void saveProject(Project project) throws SQLException {
        if (project.getId() == 0) {
            projectDAO.save(project);
        } else {
            projectDAO.update(project);
        }
    }

    private void safeReconnectDatabase(Path dbFile) throws SQLException {
        DatabaseManager dbManager = DatabaseManager.getInstance();
        try {
            dbManager.disconnect();
        } catch (SQLException ignored) {
            // ignore if there is no active connection
        }
        dbManager.connect(dbFile.toAbsolutePath().toString());
    }

    private Path createProjectDatabaseFile(String projectName) throws IOException {
        Path dataDir = Paths.get("data");
        if (Files.notExists(dataDir)) {
            Files.createDirectories(dataDir);
        }

        String safeName = sanitizeProjectName(projectName);
        Path projectFile = dataDir.resolve(safeName + ".iaedb");
        int suffix = 1;
        while (Files.exists(projectFile)) {
            projectFile = dataDir.resolve(safeName + "_" + suffix + ".iaedb");
            suffix++;
        }
        return projectFile;
    }

    private String sanitizeProjectName(String projectName) {
        String sanitized = projectName == null ? "project" : projectName.trim();
        sanitized = sanitized.replaceAll("[^\\w\\- ]", "_");
        sanitized = sanitized.replaceAll("\\s+", "_");
        if (sanitized.isEmpty()) {
            sanitized = "project";
        }
        return sanitized;
    }

    public void runProject(Project project) throws Exception {
        Configuration config = configService.findById(project.getConfigurationId())
                .orElseThrow(() -> new Exception("Configuration not found: " + project.getConfigurationId()));
        reportService.clearResults(project.getId());

        List<Path> studentDirs = zipHandler.extractAll(
                Paths.get(project.getSubmissionsDirectory()),
                Paths.get(System.getProperty("java.io.tmpdir"), "iae")
        );

        for (Path studentDir : studentDirs) {
            String studentId = studentDir.getFileName().toString();
            StudentResult result = new StudentResult();
            result.setProjectId(project.getId());
            result.setStudentId(studentId);


            if (!config.isInterpreted()) {
                ProcessResult cr = executionEngine.compile(config, studentDir);
                result.setCompileStatus(cr.isSuccess() ? "SUCCESS" : "COMPILE_ERROR");
                result.setCompileLog(resolveProcessLog("Compilation", cr));
                if (!cr.isSuccess()) {
                    result.setComparisonResult("NOT_COMPARED");
                    result.setErrorDetails(buildFailureDetails("Compilation", cr));
                    reportService.saveResult(result);
                    continue;
                }
            } else {
                result.setCompileStatus("SKIPPED");
            }


            ProcessResult rr = executionEngine.run(config, studentDir);
            result.setRunStatus(rr.isSuccess() ? "SUCCESS" : "RUNTIME_ERROR");
            result.setRunOutput(rr.getStdout());
            if (!rr.isSuccess()) {
                result.setComparisonResult("NOT_COMPARED");
                result.setErrorDetails(buildFailureDetails("Runtime", rr));
                reportService.saveResult(result);
                continue;
            }

            Path expectedOutputPath = null;
            String expectedOutputPathValue = config.getExpectedOutputPath();
            if (expectedOutputPathValue != null && !expectedOutputPathValue.isBlank()) {
                expectedOutputPath = Paths.get(expectedOutputPathValue);
            }

            if (expectedOutputPath == null) {
                result.setComparisonResult("NO_EXPECTED_OUTPUT");
                result.setErrorDetails("Expected output path is not configured for this configuration.");
            } else {
                OutputComparator.ComparisonResult comparison = outputComparator.compare(
                        rr.getStdout(),
                        expectedOutputPath
                );
                result.setComparisonResult(comparison.name());
                if (comparison == OutputComparator.ComparisonResult.FAIL) {
                    result.setErrorDetails(outputComparator.getDiffSummary(rr.getStdout(), expectedOutputPath));
                }
            }
            reportService.saveResult(result);
        }

        project.setLastRunDate(LocalDate.now().toString());
        projectDAO.update(project);
    }

    private String resolveProcessLog(String stage, ProcessResult processResult) {
        String stdout = safeText(processResult.getStdout());
        String stderr = safeText(processResult.getStderr());
        if (!stdout.isBlank() || !stderr.isBlank()) {
            return stdout + stderr;
        }
        return stage + " failed with exit code " + processResult.getExitCode() + ".";
    }

    private String buildFailureDetails(String stage, ProcessResult processResult) {
        String stdout = safeText(processResult.getStdout()).strip();
        String stderr = safeText(processResult.getStderr()).strip();

        StringBuilder message = new StringBuilder();
        message.append(stage)
                .append(" failed with exit code ")
                .append(processResult.getExitCode())
                .append('.');

        if (!stdout.isBlank()) {
            message.append(System.lineSeparator())
                    .append(System.lineSeparator())
                    .append("STDOUT:")
                    .append(System.lineSeparator())
                    .append(stdout);
        }

        if (!stderr.isBlank()) {
            message.append(System.lineSeparator())
                    .append(System.lineSeparator())
                    .append("STDERR:")
                    .append(System.lineSeparator())
                    .append(stderr);
        }

        if (stdout.isBlank() && stderr.isBlank()) {
            message.append(System.lineSeparator())
                    .append("No process output was captured.");
        }

        return message.toString();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    public List<StudentResult> getResults(int projectId) throws SQLException {
        return reportService.getResultsByProject(projectId);
    }

}
