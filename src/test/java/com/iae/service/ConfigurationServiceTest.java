package com.iae.service;

import com.iae.model.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

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

    private Configuration savePythonConfiguration(ConfigurationService service) throws Exception {
        Configuration configuration = new Configuration();
        configuration.setName("Python 3");
        configuration.setSourceFileName("main.py");
        configuration.setRunCommand("python3 main.py");
        configuration.setInterpreted(true);
        return service.saveConfiguration(configuration);
    }
}
