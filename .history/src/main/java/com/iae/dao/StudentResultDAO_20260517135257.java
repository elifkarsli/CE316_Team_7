package com.iae.dao;

import com.iae.model.StudentResult;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentResultDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();

    public void save(StudentResult r) throws SQLException {
        String sql = "INSERT INTO student_results (projectId,studentId,compileStatus,compileLog,runStatus,runOutput,comparisonResult,errorDetails) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, r.getProjectId());
            ps.setString(2, r.getStudentId());
            ps.setString(3, r.getCompileStatus());
            ps.setString(4, r.getCompileLog());
            ps.setString(5, r.getRunStatus());
            ps.setString(6, r.getRunOutput());
            ps.setString(7, r.getComparisonResult());
            ps.setString(8, r.getErrorDetails());
            ps.executeUpdate();
        }
        int id = db.getLastInsertRowId();
        if (id != -1) r.setId(id);
    }

    public List<StudentResult> findByProjectId(int projectId) throws SQLException {
        PreparedStatement ps = db.getConnection().prepareStatement("SELECT * FROM student_results WHERE projectId=?");
        ps.setInt(1, projectId);
        ResultSet rs = ps.executeQuery();
        List<StudentResult> list = new ArrayList<>();
        while (rs.next()) {
            StudentResult r = new StudentResult();
            r.setId(rs.getInt("id"));
            r.setProjectId(rs.getInt("projectId"));
            r.setStudentId(rs.getString("studentId"));
            r.setCompileStatus(rs.getString("compileStatus"));
            r.setCompileLog(rs.getString("compileLog"));
            r.setRunStatus(rs.getString("runStatus"));
            r.setRunOutput(rs.getString("runOutput"));
            r.setComparisonResult(rs.getString("comparisonResult"));
            r.setErrorDetails(rs.getString("errorDetails"));
            list.add(r);
        }
        return list;
    }

    public void deleteByProjectId(int projectId) throws SQLException {
        PreparedStatement ps = db.getConnection().prepareStatement("DELETE FROM student_results WHERE projectId=?");
        ps.setInt(1, projectId);
        ps.executeUpdate();
    }
}