package com.iae.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    public void connect(String dbFilePath) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
        connection.setAutoCommit(true);
        createTablesIfNotExist();
    }

    public Connection getConnection() { return connection; }

    public void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) connection.close();
    }

    public int getLastInsertRowId() throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }

    private void createTablesIfNotExist() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS configurations (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "name TEXT NOT NULL," +
                            "compileCommand TEXT," +
                            "runCommand TEXT," +
                            "sourceFileName TEXT," +
                            "outputFileName TEXT," +
                            "isInterpreted INTEGER DEFAULT 0," +
                            "arguments TEXT," +
                            "expectedOutputPath TEXT)"
            );
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS projects (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "name TEXT NOT NULL," +
                            "configurationId INTEGER," +
                            "submissionsDirectory TEXT," +
                            "createdDate TEXT," +
                            "lastRunDate TEXT," +
                            "FOREIGN KEY (configurationId) REFERENCES configurations(id))"
            );
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS student_results (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "projectId INTEGER," +
                            "studentId TEXT," +
                            "compileStatus TEXT," +
                            "compileLog TEXT," +
                            "runStatus TEXT," +
                            "runOutput TEXT," +
                            "comparisonResult TEXT," +
                            "errorDetails TEXT," +
                            "FOREIGN KEY (projectId) REFERENCES projects(id))"
            );
        }
    }
}