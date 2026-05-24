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
            return new ProcessResult(0, "SKIPPED", "");
        }

        String command = resolvePlaceholders(
                config.getCompileCommand(),
                config.getSourceFileName(),
                config.getOutputFileName(),
                ""
        );

        return executeCommand(tokenize(command), studentDir);
    }

    public ProcessResult run(Configuration config, Path studentDir) {
        List<String> commandTokens = tokenize(resolvePlaceholders(
                config.getRunCommand(),
                config.getSourceFileName(),
                config.getOutputFileName(),
                ""
        ));

        if (!commandTokens.isEmpty()) {
            Path executable = studentDir.resolve(commandTokens.get(0));
            commandTokens.set(0, executable.toAbsolutePath().toString());
        }

        String arguments = config.getArguments();
        if (arguments != null && !arguments.isBlank()) {
            for (String arg : arguments.trim().split("\\s+")) {
                if (!arg.isEmpty()) {
                    commandTokens.add(arg);
                }
            }
        }

        return executeCommand(commandTokens, studentDir);
    }

    private ProcessResult executeCommand(List<String> tokens, Path workingDir) {
        if (tokens == null || tokens.isEmpty()) {
            return new ProcessResult(-1, "", "No command provided.");
        }

        ProcessBuilder pb = new ProcessBuilder(tokens);
        pb.directory(workingDir.toFile());
        pb.redirectErrorStream(false);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            return new ProcessResult(-1, "", "Failed to start process: " + e.getMessage());
        }

        StringBuilder stdoutBuilder = new StringBuilder();
        StringBuilder stderrBuilder = new StringBuilder();

        Thread stdoutThread = new Thread(() -> readStream(process.getInputStream(), stdoutBuilder));
        Thread stderrThread = new Thread(() -> readStream(process.getErrorStream(), stderrBuilder));

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
        return new ProcessResult(exitCode, stdoutBuilder.toString(), stderrBuilder.toString());
    }

    private void readStream(java.io.InputStream inputStream, StringBuilder builder) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        } catch (IOException e) {
            builder.append("Error reading process output: ").append(e.getMessage());
        }
    }

    private List<String> tokenize(String command) {
        List<String> tokens = new ArrayList<>();
        if (command == null) {
            return tokens;
        }

        for (String token : command.trim().split("\\s+")) {
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String resolvePlaceholders(String template,
                                       String sourceFileName,
                                       String outputFileName,
                                       String arguments) {
        String resolved = template == null ? "" : template;

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
