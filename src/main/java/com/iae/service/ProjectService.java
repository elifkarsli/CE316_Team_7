package com.iae.service;

import com.iae.dao.DatabaseManager;
import com.iae.dao.ProjectDAO;
import com.iae.dao.ConfigurationDAO;
import com.iae.dao.StudentResultDAO;
import com.iae.model.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ProjectService {
    private ProjectDAO projectDAO = new ProjectDAO();
    private ConfigurationDAO configDAO = new ConfigurationDAO();
    private StudentResultDAO resultDAO = new StudentResultDAO();
    private ZipHandler zipHandler = new ZipHandler();
    private ExecutionEngine executionEngine = new ExecutionEngine();
    private OutputComparator outputComparator = new OutputComparator();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Project createProject(String name, int configId, Path submissionsDir) throws Exception {
        Project p = new Project();
        p.setName(name);
        p.setConfigurationId(configId);
        p.setSubmissionsDirectory(submissionsDir.toString());
        p.setCreatedDate(LocalDate.now().toString());
        projectDAO.save(p);
        return p;
    }

    public Project openProject(Path dbFile) throws Exception {
        DatabaseManager.getInstance().connect(dbFile.toString());
        List<Project> projects = projectDAO.findAll();
        if (projects.isEmpty()) throw new Exception("No project found in this file.");
        return projects.get(0);
    }

    public void runProject(Project project) throws Exception {
        Configuration config = configDAO.findById(project.getConfigurationId());
        resultDAO.deleteByProjectId(project.getId());

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
                    resultDAO.save(result);
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
                resultDAO.save(result);
                continue;
            }


            String verdict = outputComparator.compare(
                    rr.getStdout(),
                    Paths.get(config.getExpectedOutputPath())
            ).toString();
            result.setComparisonResult(verdict);
            resultDAO.save(result);
        }

        project.setLastRunDate(LocalDate.now().toString());
        projectDAO.update(project);
    }
    public List<StudentResult> getResults(int projectId) throws SQLException {
        // TODO (Tina): Replace with:
        //   return new ReportService().getResultsByProject(projectId);
        return new StudentResultDAO().findByProjectId(projectId);
    }

}