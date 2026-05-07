package com.iae.model;

public class Configuration {
    private int id;
    private String name;
    private String compileCommand;
    private String runCommand;
    private String sourceFileName;
    private String outputFileName;
    private boolean interpreted;
    private String arguments;
    private String expectedOutputPath;

    public Configuration() {
    }

    public Configuration(int id, String name, String compileCommand, String runCommand,
                         String sourceFileName, String outputFileName, boolean interpreted,
                         String arguments, String expectedOutputPath) {
        this.id = id;
        this.name = name;
        this.compileCommand = compileCommand;
        this.runCommand = runCommand;
        this.sourceFileName = sourceFileName;
        this.outputFileName = outputFileName;
        this.interpreted = interpreted;
        this.arguments = arguments;
        this.expectedOutputPath = expectedOutputPath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompileCommand() {
        return compileCommand;
    }

    public void setCompileCommand(String compileCommand) {
        this.compileCommand = compileCommand;
    }

    public String getRunCommand() {
        return runCommand;
    }

    public void setRunCommand(String runCommand) {
        this.runCommand = runCommand;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public String getOutputFileName() {
        return outputFileName;
    }

    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }

    public boolean isInterpreted() {
        return interpreted;
    }

    public void setInterpreted(boolean interpreted) {
        this.interpreted = interpreted;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    public String getExpectedOutputPath() {
        return expectedOutputPath;
    }

    public void setExpectedOutputPath(String expectedOutputPath) {
        this.expectedOutputPath = expectedOutputPath;
    }

    @Override
    public String toString() {
        return name;
    }
}
