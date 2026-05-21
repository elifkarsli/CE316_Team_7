package com.iae.service;

import com.iae.model.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void deleteConfigurationRemovesUnusedConfiguration() throws Exception {
        Path configurationsFile = tempDir.resolve("configurations.json");
        Path projectsFile = tempDir.resolve("projects.json");
        ConfigurationService service = new ConfigurationService(configurationsFile, projectsFile);
        Configuration configuration = savePythonConfiguration(service);

        boolean deleted = service.deleteConfiguration(configuration.getId());

        assertTrue(deleted);
        assertTrue(service.listConfigurations().isEmpty());
    }

    @Test
    void deleteConfigurationRejectsConfigurationUsedByProject() throws Exception {
        Path configurationsFile = tempDir.resolve("configurations.json");
        Path projectsFile = tempDir.resolve("projects.json");
        ConfigurationService service = new ConfigurationService(configurationsFile, projectsFile);
        Configuration configuration = savePythonConfiguration(service);
        Files.writeString(projectsFile, """
                [
                  {
                    "name": "Assignment 1",
                    "configurationId": %d
                  }
                ]
                """.formatted(configuration.getId()));

        assertThrows(IllegalStateException.class,
                () -> service.deleteConfiguration(configuration.getId()));
        assertFalse(service.listConfigurations().isEmpty());
    }

    @Test
    void importConfigurationsRejectsBrokenJson() throws Exception {
        Path configurationsFile = tempDir.resolve("configurations.json");
        Path projectsFile = tempDir.resolve("projects.json");
        Path importFile = tempDir.resolve("broken.json");
        ConfigurationService service = new ConfigurationService(configurationsFile, projectsFile);
        Files.writeString(importFile, "{ broken json");

        assertThrows(IOException.class, () -> service.importConfigurations(importFile));
        assertTrue(service.listConfigurations().isEmpty());
    }

    @Test
    void importConfigurationsSkipsDuplicateNames() throws Exception {
        Path configurationsFile = tempDir.resolve("configurations.json");
        Path projectsFile = tempDir.resolve("projects.json");
        Path importFile = tempDir.resolve("duplicate.json");
        ConfigurationService service = new ConfigurationService(configurationsFile, projectsFile);
        savePythonConfiguration(service);
        Files.writeString(importFile, """
                {
                  "name": " python 3 ",
                  "runCommand": "python3 main.py",
                  "sourceFileName": "main.py",
                  "interpreted": true
                }
                """);

        List<Configuration> importedConfigurations = service.importConfigurations(importFile);

        assertTrue(importedConfigurations.isEmpty());
        assertEquals(1, service.listConfigurations().size());
    }

    @Test
    void importConfigurationsRejectsMissingRequiredFields() throws Exception {
        Path configurationsFile = tempDir.resolve("configurations.json");
        Path projectsFile = tempDir.resolve("projects.json");
        Path importFile = tempDir.resolve("missing-fields.json");
        ConfigurationService service = new ConfigurationService(configurationsFile, projectsFile);
        Files.writeString(importFile, """
                {
                  "name": "Java 17",
                  "sourceFileName": "Main.java",
                  "interpreted": false
                }
                """);

        assertThrows(IllegalArgumentException.class, () -> service.importConfigurations(importFile));
        assertTrue(service.listConfigurations().isEmpty());
    }

    private Configuration savePythonConfiguration(ConfigurationService service) throws Exception {
        Configuration configuration = new Configuration();
        configuration.setName("Python 3");
        configuration.setSourceFileName("main.py");
        configuration.setRunCommand("python3 main.py");
        configuration.setInterpreted(true);
        return service.saveConfiguration(configuration);
    }
}
