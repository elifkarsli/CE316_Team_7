package com.iae.dao;

import com.iae.model.Project;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();

    public void save(Project p) throws SQLException {
        String sql = "INSERT INTO projects (name,configurationId,submissionsDirectory,createdDate,lastRunDate) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setInt(2, p.getConfigurationId());
            ps.setString(3, p.getSubmissionsDirectory());
            ps.setString(4, p.getCreatedDate());
            ps.setString(5, p.getLastRunDate());
            ps.executeUpdate();
        }
        int id = db.getLastInsertRowId();
        if (id != -1) p.setId(id);
    }

    public void update(Project p) throws SQLException {
        String sql = "UPDATE projects SET name=?,configurationId=?,submissionsDirectory=?,createdDate=?,lastRunDate=? WHERE id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setInt(2, p.getConfigurationId());
            ps.setString(3, p.getSubmissionsDirectory());
            ps.setString(4, p.getCreatedDate());
            ps.setString(5, p.getLastRunDate());
            ps.setInt(6, p.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = db.getConnection().prepareStatement("DELETE FROM projects WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Project> findAll() throws SQLException {
        Statement stmt = db.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM projects");
        List<Project> list = new ArrayList<>();
        while (rs.next()) {
            Project p = new Project();
            p.setId(rs.getInt("id"));
            p.setName(rs.getString("name"));
            p.setConfigurationId(rs.getInt("configurationId"));
            p.setSubmissionsDirectory(rs.getString("submissionsDirectory"));
            p.setCreatedDate(rs.getString("createdDate"));
            p.setLastRunDate(rs.getString("lastRunDate"));
            list.add(p);
        }
        return list;
    }

    public Project findById(int id) throws SQLException {
        try (PreparedStatement ps = db.getConnection().prepareStatement("SELECT * FROM projects WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Project p = new Project();
                p.setId(rs.getInt("id"));
                p.setName(rs.getString("name"));
                p.setConfigurationId(rs.getInt("configurationId"));
                p.setSubmissionsDirectory(rs.getString("submissionsDirectory"));
                p.setCreatedDate(rs.getString("createdDate"));
                p.setLastRunDate(rs.getString("lastRunDate"));
                return p;
            }
        }
        return null;
    }
}