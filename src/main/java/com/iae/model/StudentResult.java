package com.iae.model;

public class StudentResult {
    private int id;
    private int projectId;
    private String studentId;
    private String compileStatus;
    private String compileLog;
    private String runStatus;
    private String runOutput;
    private String comparisonResult;
    private String errorDetails;

    public StudentResult() {}

    public StudentResult(int id, int projectId, String studentId, String compileStatus,
                         String compileLog, String runStatus, String runOutput,
                         String comparisonResult, String errorDetails) {
        this.id = id;
        this.projectId = projectId;
        this.studentId = studentId;
        this.compileStatus = compileStatus;
        this.compileLog = compileLog;
        this.runStatus = runStatus;
        this.runOutput = runOutput;
        this.comparisonResult = comparisonResult;
        this.errorDetails = errorDetails;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getCompileStatus() { return compileStatus; }
    public void setCompileStatus(String compileStatus) { this.compileStatus = compileStatus; }

    public String getCompileLog() { return compileLog; }
    public void setCompileLog(String compileLog) { this.compileLog = compileLog; }

    public String getRunStatus() { return runStatus; }
    public void setRunStatus(String runStatus) { this.runStatus = runStatus; }

    public String getRunOutput() { return runOutput; }
    public void setRunOutput(String runOutput) { this.runOutput = runOutput; }

    public String getComparisonResult() { return comparisonResult; }
    public void setComparisonResult(String comparisonResult) { this.comparisonResult = comparisonResult; }

    public String getErrorDetails() { return errorDetails; }
    public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }
}