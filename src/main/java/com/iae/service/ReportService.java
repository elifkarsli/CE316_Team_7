package com.iae.service;

import com.iae.dao.StudentResultDAO;
import com.iae.model.StudentResult;

import java.sql.SQLException;
import java.util.List;

public class ReportService {
    private final StudentResultDAO dao = new StudentResultDAO();

    public void saveResult(StudentResult result) throws SQLException {
        dao.save(result);
    }

    public List<StudentResult> getResultsByProject(int projectId) throws SQLException {
        return dao.findByProjectId(projectId);
    }

    public void clearResults(int projectId) throws SQLException {
        dao.deleteByProjectId(projectId);
    }
}
