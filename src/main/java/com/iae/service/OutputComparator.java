package com.iae.service;

import java.nio.file.Path;

public class OutputComparator {
    public enum Verdict { PASS, FAIL }

    public Verdict compare(String actualOutput, Path expectedOutputPath) throws Exception {
        // Tina will implement this
        return Verdict.FAIL;
    }
}