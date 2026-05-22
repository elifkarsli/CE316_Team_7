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
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataDir, "*.iaedb")) {
            for (Path dbFile : stream) {
                Optional<Project> project = readProjectFromDatabase(dbFile);
                project.ifPresent(value -> savedProjects.add(new SavedProjectInfo(value.getName(), dbFile)));
            }
        }

        return savedProjects;
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
        safeReconnectDatabase(dbFile);
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
                result.setCompileLog(cr.getStdout() + cr.getStderr());
                if (!cr.isSuccess()) {
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
                result.setErrorDetails(rr.getStderr());
                reportService.saveResult(result);
                continue;
            }

            Path expectedOutputPath = Paths.get(config.getExpectedOutputPath());
            OutputComparator.ComparisonResult comparison = outputComparator.compare(
                    rr.getStdout(),
                    expectedOutputPath
            );
            result.setComparisonResult(comparison.name());
            if (comparison == OutputComparator.ComparisonResult.FAIL) {
                result.setErrorDetails(outputComparator.getDiffSummary(rr.getStdout(), expectedOutputPath));
            }
            reportService.saveResult(result);
        }

        project.setLastRunDate(LocalDate.now().toString());
        projectDAO.update(project);
    }
    public List<StudentResult> getResults(int projectId) throws SQLException {
        return reportService.getResultsByProject(projectId);
    }

}
