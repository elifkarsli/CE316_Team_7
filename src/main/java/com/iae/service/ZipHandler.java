package com.iae.service;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class ZipHandler {

    public List<Path> extractAll(Path zipDirectory, Path outputBaseDir) throws IOException {
        List<Path> studentDirs = new ArrayList<>();

        // Get all .zip files in the directory
        File[] zipFiles = zipDirectory.toFile().listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".zip"));

        if (zipFiles == null || zipFiles.length == 0) {
            System.out.println("No ZIP files found in: " + zipDirectory);
            return studentDirs;
        }

        for (File zipFile : zipFiles) {
            try {
                Path studentDir = extractOne(zipFile.toPath(), outputBaseDir);
                studentDirs.add(studentDir);
            } catch (IOException e) {
                System.err.println("Failed to extract: " + zipFile.getName() + " -> " + e.getMessage());
            }
        }
        return studentDirs;
    }

    public Path extractOne(Path zipFile, Path outputBaseDir) throws IOException {
        String studentId = getStudentId(zipFile);
        Path studentDir = outputBaseDir.resolve(studentId);

        // Create new directory (delete if already exists)
        if (Files.exists(studentDir)) {
            deleteDirectory(studentDir.toFile());
        }
        Files.createDirectories(studentDir);

        // Extract all entries
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue; // skip directory entries

                // Security check: prevent path traversal attack
                Path entryPath = studentDir.resolve(entry.getName()).normalize();
                if (!entryPath.startsWith(studentDir)) {
                    System.err.println("Skipping suspicious path: " + entry.getName());
                    continue;
                }

                // Create parent directories if needed
                Files.createDirectories(entryPath.getParent());

                // Write the file
                try (OutputStream os = new FileOutputStream(entryPath.toFile())) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = zis.read(buffer)) > 0) os.write(buffer, 0, len);
                }
                zis.closeEntry();
            }
        }
        return studentDir;
    }

    public String getStudentId(Path zipFile) {
        String name = zipFile.getFileName().toString();
        return name.endsWith(".zip") ? name.substring(0, name.length() - 4) : name;
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f);
                else f.delete();
            }
        }
        dir.delete();
    }
}