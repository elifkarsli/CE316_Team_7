package com.iae.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipHandler {

    public List<Path> extractAll(Path zipDirectory, Path outputBaseDir) throws IOException {
        List<Path> studentDirs = new ArrayList<>();

        File[] zipFiles = zipDirectory.toFile().listFiles(
                f -> f.isFile() && f.getName().toLowerCase().endsWith(".zip"));
        if (zipFiles == null || zipFiles.length == 0) {
            return studentDirs;
        }

        for (File zipFile : zipFiles) {
            try {
                studentDirs.add(extractOne(zipFile.toPath(), outputBaseDir));
            } catch (IOException ignored) {
                // Skip bad archives and keep processing the rest.
            }
        }
        return studentDirs;
    }

    public Path extractOne(Path zipFile, Path outputBaseDir) throws IOException {
        String studentId = getStudentId(zipFile);
        Path studentDir = outputBaseDir.resolve(studentId);

        if (Files.exists(studentDir)) {
            deleteDirectory(studentDir.toFile());
        }
        Files.createDirectories(studentDir);

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                Path entryPath = studentDir.resolve(entry.getName()).normalize();
                if (!entryPath.startsWith(studentDir)) {
                    continue;
                }

                Files.createDirectories(entryPath.getParent());

                try (OutputStream os = new FileOutputStream(entryPath.toFile())) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        os.write(buffer, 0, len);
                    }
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
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }
}
