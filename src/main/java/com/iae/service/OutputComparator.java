package com.iae.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class OutputComparator {
    public enum ComparisonResult { PASS, FAIL }

    public ComparisonResult compare(String actualOutput, Path expectedOutputPath) {
        try {
            String expected = Files.readString(expectedOutputPath);
            String normalizedActual = normalize(actualOutput);
            String normalizedExpected = normalize(expected);
            return normalizedActual.equals(normalizedExpected)
                    ? ComparisonResult.PASS
                    : ComparisonResult.FAIL;
        } catch (IOException e) {
            return ComparisonResult.FAIL;
        }
    }

    public String getDiffSummary(String actual, Path expectedOutputPath) {
        try {
            String expected = normalize(Files.readString(expectedOutputPath));
            String normalizedActual = normalize(actual);
            if (normalizedActual.equals(expected)) {
                return "Output matches expected.";
            }
            return "EXPECTED:\n" + expected + "\n\nACTUAL:\n" + normalizedActual;
        } catch (IOException e) {
            return "Could not read expected file: " + e.getMessage();
        }
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        text = text.replace("\r\n", "\n");
        text = text.replace('\r', '\n');

        String[] lines = text.split("\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line.stripTrailing()).append('\n');
        }

        return sb.toString().strip();
    }
}
