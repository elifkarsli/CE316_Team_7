package com.iae.service;

import com.iae.model.Configuration;
import com.iae.model.ProcessResult;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ExecutionEngine {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    public ProcessResult compile(Configuration config, Path studentDir) {
        if (config.isInterpreted()) {
            System.out.println("Skipping compilation for interpreted language: " + config.getName());
            return new ProcessResult(0, "SKIPPED", "");
        }

        String command = resolvePlaceholders(
                config.getCompileCommand(),
                config.getSourceFileName(),
                config.getOutputFileName(),
                ""
        );

        System.out.println("Compiling: " + command);
        return executeCommand(command, studentDir);
    }

    public ProcessResult run(Configuration config, Path studentDir) {

        List<String> commandList = new ArrayList<>();

        String command = resolvePlaceholders(
                config.getRunCommand(),
                config.getSourceFileName(),
                config.getOutputFileName(),
                ""
        );

        String[] parts = command.trim().split("\\s+");
        boolean first = true;
        for (String token : parts) {
            if (token.isEmpty()) continue;
            if (first) {
                // Resolve the executable against the working directory
                Path exePath = studentDir.resolve(token);
                commandList.add(exePath.toAbsolutePath().toString());
                first = false;
            } else {
                commandList.add(token);
            }
        }

        String arguments = config.getArguments();
        if (arguments != null && !arguments.isBlank()) {
            for (String arg : arguments.trim().split("\\s+")) {
                if (!arg.isEmpty()) commandList.add(arg);
            }
        }
        System.out.println("Running: " + command);
        return executeCommandList(commandList, studentDir);
    }

    private ProcessResult executeCommandList(List<String> tokens, Path workingDir) {
        ProcessBuilder pb = new ProcessBuilder(tokens);
        pb.directory(workingDir.toFile());
        pb.redirectErrorStream(false);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            System.out.println("WARNING: Failed to start process: " + e.getMessage());
            return new ProcessResult(-1, "", "Failed to start process: " + e.getMessage());
        }

        StringBuilder stdoutBuilder = new StringBuilder();
        StringBuilder stderrBuilder = new StringBuilder();

        Thread stdoutThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdoutBuilder.append(line).append("\n");
                }
            } catch (IOException e) {
                System.out.println("WARNING: Error reading stdout");
            }
        });

        Thread stderrThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderrBuilder.append(line).append("\n");
                }
            } catch (IOException e) {
                System.out.println("WARNING: Error reading stderr");
            }
        });

        stdoutThread.start();
        stderrThread.start();

        boolean finished;
        try {
            finished = process.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return new ProcessResult(-1, "", "Process interrupted: " + e.getMessage());
        }

        if (!finished) {
            process.destroyForcibly();
            System.out.println("WARNING: Process timed out after " + DEFAULT_TIMEOUT_SECONDS + "s");
            return new ProcessResult(-1, stdoutBuilder.toString(),
                    "TIMEOUT: Process exceeded " + DEFAULT_TIMEOUT_SECONDS + " second limit.");
        }

        try {
            stdoutThread.join(2000);
            stderrThread.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        int exitCode = process.exitValue();
        System.out.println("Process finished with exit code: " + exitCode);
        return new ProcessResult(exitCode, stdoutBuilder.toString(), stderrBuilder.toString());
    }
        private ProcessResult executeCommand(String command, Path workingDir) {

        List<String> tokens = new ArrayList<>();
        for (String token : command.trim().split("\\s+")) {
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }

        ProcessBuilder pb = new ProcessBuilder(tokens);
        pb.directory(workingDir.toFile());

        // Holds stdout and stderr separately
        pb.redirectErrorStream(false);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            System.out.println("WARNING: " + "Failed to start process: " + command);
            return new ProcessResult(-1, "", "Failed to start process: " + e.getMessage());
        }

        // Capture stdout and stderr concurrently using background threads.
        StringBuilder stdoutBuilder = new StringBuilder();
        StringBuilder stderrBuilder = new StringBuilder();

        Thread stdoutThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdoutBuilder.append(line).append("\n");
                }
            } catch (IOException e) {
                System.out.println("WARNING: " + "Error reading stdout");
            }
        });

        Thread stderrThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderrBuilder.append(line).append("\n");
                }
            } catch (IOException e) {
                System.out.println("WARNING: " + "Error reading stderr");
            }
        });

        stdoutThread.start();
        stderrThread.start();

        // Wait for the process to finish within the timeout
        boolean finished;
        try {
            finished = process.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return new ProcessResult(-1, "", "Process interrupted: " + e.getMessage());
        }

        if (!finished) {
            process.destroyForcibly();
            System.out.println("WARNING: " + "Process timed out after " + DEFAULT_TIMEOUT_SECONDS + "s: " + command);
            return new ProcessResult(-1, stdoutBuilder.toString(),
                    "TIMEOUT: Process exceeded " + DEFAULT_TIMEOUT_SECONDS + " second limit.");
        }

        // Give reader threads a moment to flush remaining output
        try {
            stdoutThread.join(2000);
            stderrThread.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        int exitCode = process.exitValue();
        System.out.println("Process finished with exit code: " + exitCode);
        return new ProcessResult(exitCode, stdoutBuilder.toString(), stderrBuilder.toString());
    }

    public String captureOutput(Process process, int timeoutSeconds) {
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;
            long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);

            while (System.currentTimeMillis() < deadline && (line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

        } catch (IOException e) {
            System.out.println("WARNING: " + "Error reading process output: " + e.getMessage());
            output.append("Error reading output: ").append(e.getMessage());
        }

        return output.toString();
    }

    private String resolvePlaceholders(String template,
                                       String sourceFileName,
                                       String outputFileName,
                                       String arguments) {
        String resolved = template;

        resolved = resolved.replace("{sourceFile}", sourceFileName != null ? sourceFileName : "");
        resolved = resolved.replace("{outputFile}", outputFileName != null ? outputFileName : "");

        if (resolved.contains("{args}")) {
            resolved = resolved.replace("{args}", arguments != null ? arguments : "");
        } else if (arguments != null && !arguments.isBlank()) {
            resolved = resolved + " " + arguments;
        }
            return resolved;
    }
}