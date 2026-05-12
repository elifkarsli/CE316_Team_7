package com.iae.dao;

import com.iae.model.Configuration;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigurationDAO {
    private DatabaseManager db = DatabaseManager.getInstance();

    public void save(Configuration c) throws SQLException {
        String sql = "INSERT INTO configurations (name,compileCommand,runCommand,sourceFileName,outputFileName,isInterpreted,arguments,expectedOutputPath) VALUES (?,?,?,?,?,?,?,?)";
        PreparedStatement ps = db.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, c.getName());
        ps.setString(2, c.getCompileCommand());
        ps.setString(3, c.getRunCommand());
        ps.setString(4, c.getSourceFileName());
        ps.setString(5, c.getOutputFileName());
        ps.setInt(6, c.isInterpreted() ? 1 : 0);
        ps.setString(7, c.getArguments());
        ps.setString(8, c.getExpectedOutputPath());
        ps.executeUpdate();
        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) c.setId(keys.getInt(1));
    }

    public void update(Configuration c) throws SQLException {
        String sql = "UPDATE configurations SET name=?,compileCommand=?,runCommand=?,sourceFileName=?,outputFileName=?,isInterpreted=?,arguments=?,expectedOutputPath=? WHERE id=?";
        PreparedStatement ps = db.getConnection().prepareStatement(sql);
        ps.setString(1, c.getName());
        ps.setString(2, c.getCompileCommand());
        ps.setString(3, c.getRunCommand());
        ps.setString(4, c.getSourceFileName());
        ps.setString(5, c.getOutputFileName());
        ps.setInt(6, c.isInterpreted() ? 1 : 0);
        ps.setString(7, c.getArguments());
        ps.setString(8, c.getExpectedOutputPath());
        ps.setInt(9, c.getId());
        ps.executeUpdate();
    }

    public void delete(int id) throws SQLException {
        PreparedStatement ps = db.getConnection().prepareStatement("DELETE FROM configurations WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public List<Configuration> findAll() throws SQLException {
        Statement stmt = db.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM configurations");
        List<Configuration> list = new ArrayList<>();
        while (rs.next()) {
            Configuration c = new Configuration();
            c.setId(rs.getInt("id"));
            c.setName(rs.getString("name"));
            c.setCompileCommand(rs.getString("compileCommand"));
            c.setRunCommand(rs.getString("runCommand"));
            c.setSourceFileName(rs.getString("sourceFileName"));
            c.setOutputFileName(rs.getString("outputFileName"));
            c.setInterpreted(rs.getInt("isInterpreted") == 1);
            c.setArguments(rs.getString("arguments"));
            c.setExpectedOutputPath(rs.getString("expectedOutputPath"));
            list.add(c);
        }
        return list;
    }

    public Configuration findById(int id) throws SQLException {
        PreparedStatement ps = db.getConnection().prepareStatement("SELECT * FROM configurations WHERE id=?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Configuration c = new Configuration();
            c.setId(rs.getInt("id"));
            c.setName(rs.getString("name"));
            c.setCompileCommand(rs.getString("compileCommand"));
            c.setRunCommand(rs.getString("runCommand"));
            c.setSourceFileName(rs.getString("sourceFileName"));
            c.setOutputFileName(rs.getString("outputFileName"));
            c.setInterpreted(rs.getInt("isInterpreted") == 1);
            c.setArguments(rs.getString("arguments"));
            c.setExpectedOutputPath(rs.getString("expectedOutputPath"));
            return c;
        }
        return null;
    }
}