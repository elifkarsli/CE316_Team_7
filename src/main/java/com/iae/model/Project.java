package com.iae.model;

public class Project {
    private int id;
    private String name;
    private int configurationId;
    private String submissionsDirectory;
    private String createdDate;
    private String lastRunDate;

    public Project() {}

    public Project(int id, String name, int configurationId, String submissionsDirectory,
                   String createdDate, String lastRunDate) {
        this.id = id;
        this.name = name;
        this.configurationId = configurationId;
        this.submissionsDirectory = submissionsDirectory;
        this.createdDate = createdDate;
        this.lastRunDate = lastRunDate;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getConfigurationId() { return configurationId; }
    public void setConfigurationId(int configurationId) { this.configurationId = configurationId; }

    public String getSubmissionsDirectory() { return submissionsDirectory; }
    public void setSubmissionsDirectory(String submissionsDirectory) { this.submissionsDirectory = submissionsDirectory; }

    public String getCreatedDate() { return createdDate; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }

    public String getLastRunDate() { return lastRunDate; }
    public void setLastRunDate(String lastRunDate) { this.lastRunDate = lastRunDate; }

    @Override
    public String toString() { return name; }
}