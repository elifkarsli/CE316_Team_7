package com.iae.model;

public class ProcessResult {
    private int exitCode;
    private String stdout;
    private String stderr;

    public ProcessResult(int exitCode, String stdout, String stderr) {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public int getExitCode() { return exitCode; }
    public String getStdout() { return stdout; }
    public String getStderr() { return stderr; }
    public boolean isSuccess() { return exitCode == 0; }
}