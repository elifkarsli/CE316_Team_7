package com.iae;

import com.iae.service.OutputComparator;
import com.iae.service.OutputComparator.ComparisonResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

class OutputComparatorTest {
    private final OutputComparator comparator = new OutputComparator();

    @Test
    void testExactMatch() throws Exception {
        Path expected = Files.createTempFile("expected", ".txt");
        try {
            Files.writeString(expected, "apple\nbanana\ncherry\n");
            ComparisonResult result = comparator.compare("apple\nbanana\ncherry\n", expected);
            Assertions.assertEquals(ComparisonResult.PASS, result);
        } finally {
            Files.deleteIfExists(expected);
        }
    }

    @Test
    void testDifferentOutput() throws Exception {
        Path expected = Files.createTempFile("expected", ".txt");
        try {
            Files.writeString(expected, "apple\nbanana\n");
            ComparisonResult result = comparator.compare("banana\napple\n", expected);
            Assertions.assertEquals(ComparisonResult.FAIL, result);
        } finally {
            Files.deleteIfExists(expected);
        }
    }

    @Test
    void testWindowsLineEndings() throws Exception {
        Path expected = Files.createTempFile("expected", ".txt");
        try {
            Files.writeString(expected, "hello\nworld");
            ComparisonResult result = comparator.compare("hello\r\nworld", expected);
            Assertions.assertEquals(ComparisonResult.PASS, result);
        } finally {
            Files.deleteIfExists(expected);
        }
    }

    @Test
    void testTrailingWhitespaceIgnored() throws Exception {
        Path expected = Files.createTempFile("expected", ".txt");
        try {
            Files.writeString(expected, "hello\nworld\n");
            ComparisonResult result = comparator.compare("hello  \nworld\t \n", expected);
            Assertions.assertEquals(ComparisonResult.PASS, result);
        } finally {
            Files.deleteIfExists(expected);
        }
    }

    @Test
    void testMissingExpectedFile() throws Exception {
        Path missingParent = Files.createTempDirectory("missing-expected");
        try {
            Path missing = missingParent.resolve("missing.txt");
            ComparisonResult result = comparator.compare("hello\nworld\n", missing);
            Assertions.assertEquals(ComparisonResult.FAIL, result);
        } finally {
            Files.deleteIfExists(missingParent);
        }
    }
}
