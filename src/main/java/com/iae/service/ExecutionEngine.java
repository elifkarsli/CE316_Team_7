package com.iae.service;

import com.iae.model.Configuration;
import com.iae.model.ProcessResult;
import java.nio.file.Path;

public class ExecutionEngine {
    public ProcessResult compile(Configuration config, Path studentDir) throws Exception {
        // Duru will implement this
        return new ProcessResult(0, "", "");
    }

    public ProcessResult run(Configuration config, Path studentDir) throws Exception {
        // Duru will implement this
        return new ProcessResult(0, "", "");
    }
}