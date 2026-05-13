package com.iae.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.iae.dao.ConfigurationDAO;
import com.iae.dao.ProjectDAO;
import com.iae.model.Configuration;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ConfigurationService {
    private static final Type CONFIGURATION_LIST_TYPE = new TypeToken<List<Configuration>>() {
    }.getType();
    private final ProjectDAO       projectDAO = new ProjectDAO();
    private final ConfigurationDAO configDAO  = new ConfigurationDAO();
    private final Path storageFile;
    private final Gson gson;
    public ConfigurationService() {
        this(Path.of("data", "configurations.json"));
    }

    public ConfigurationService(Path storageFile) {
        this.storageFile = storageFile;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public List<Configuration> listConfigurations() throws IOException {
        List<Configuration> configurations = readConfigurations();
        configurations.sort(Comparator.comparing(Configuration::getName, String.CASE_INSENSITIVE_ORDER));
        return configurations;
    }

    public Optional<Configuration> findById(int id) throws IOException {
        return readConfigurations().stream()
                .filter(configuration -> configuration.getId() == id)
                .findFirst();
    }
    public Configuration getById(int id) throws SQLException {
        return configDAO.findById(id);
    }

    public Configuration saveConfiguration(Configuration configuration) throws IOException {
        validateConfiguration(configuration);

        List<Configuration> configurations = readConfigurations();
        configuration.setId(nextId(configurations));
        configurations.add(configuration);
        writeConfigurations(configurations);
        return configuration;
    }

    public Configuration updateConfiguration(Configuration updatedConfiguration) throws IOException {
        validateConfiguration(updatedConfiguration);

        List<Configuration> configurations = readConfigurations();
        for (int i = 0; i < configurations.size(); i++) {
            if (configurations.get(i).getId() == updatedConfiguration.getId()) {
                configurations.set(i, updatedConfiguration);
                writeConfigurations(configurations);
                return updatedConfiguration;
            }
        }

        throw new IllegalArgumentException("Configuration not found: " + updatedConfiguration.getId());
    }

    public boolean deleteConfiguration(int id) throws IOException {
        List<Configuration> configurations = readConfigurations();
        boolean removed = configurations.removeIf(configuration -> configuration.getId() == id);

        if (removed) {
            writeConfigurations(configurations);
        }

        return removed;
    }

    public void exportConfiguration(int id, Path exportFile) throws IOException {
        Configuration configuration = findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found: " + id));

        createParentDirectories(exportFile);
        try (Writer writer = Files.newBufferedWriter(exportFile)) {
            gson.toJson(configuration, writer);
        }
    }

    public List<Configuration> importConfigurations(Path importFile) throws IOException {
        List<Configuration> importedConfigurations = readImportedConfigurations(importFile);
        List<Configuration> configurations = readConfigurations();
        List<Configuration> savedConfigurations = new ArrayList<>();

        int nextId = nextId(configurations);
        for (Configuration importedConfiguration : importedConfigurations) {
            validateConfiguration(importedConfiguration);
            importedConfiguration.setId(nextId++);
            configurations.add(importedConfiguration);
            savedConfigurations.add(importedConfiguration);
        }

        writeConfigurations(configurations);
        return savedConfigurations;
    }

    private List<Configuration> readConfigurations() throws IOException {
        if (Files.notExists(storageFile)) {
            return new ArrayList<>();
        }

        try (Reader reader = Files.newBufferedReader(storageFile)) {
            List<Configuration> configurations = gson.fromJson(reader, CONFIGURATION_LIST_TYPE);
            return configurations == null ? new ArrayList<>() : configurations;
        } catch (JsonParseException exception) {
            throw new IOException("Could not read configurations JSON file.", exception);
        }
    }

    private List<Configuration> readImportedConfigurations(Path importFile) throws IOException {
        try (Reader reader = Files.newBufferedReader(importFile)) {
            Configuration singleConfiguration = gson.fromJson(reader, Configuration.class);
            if (singleConfiguration != null && singleConfiguration.getName() != null) {
                return new ArrayList<>(List.of(singleConfiguration));
            }
        } catch (JsonParseException ignored) {
            // Try list format below.
        }

        try (Reader reader = Files.newBufferedReader(importFile)) {
            List<Configuration> configurations = gson.fromJson(reader, CONFIGURATION_LIST_TYPE);
            return configurations == null ? new ArrayList<>() : configurations;
        } catch (JsonParseException exception) {
            throw new IOException("Could not import configuration JSON file.", exception);
        }
    }

    private void writeConfigurations(List<Configuration> configurations) throws IOException {
        createParentDirectories(storageFile);

        Path temporaryFile = storageFile.resolveSibling(storageFile.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporaryFile)) {
            gson.toJson(configurations, writer);
        }

        Files.move(temporaryFile, storageFile, StandardCopyOption.REPLACE_EXISTING);
    }

    private void validateConfiguration(Configuration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be empty.");
        }
        if (isBlank(configuration.getName())) {
            throw new IllegalArgumentException("Configuration name is required.");
        }
        if (isBlank(configuration.getRunCommand())) {
            throw new IllegalArgumentException("Run command is required.");
        }
        if (!configuration.isInterpreted() && isBlank(configuration.getCompileCommand())) {
            throw new IllegalArgumentException("Compile command is required for compiled languages.");
        }
        if (isBlank(configuration.getSourceFileName())) {
            throw new IllegalArgumentException("Source file name is required.");
        }
    }

    private int nextId(List<Configuration> configurations) {
        return configurations.stream()
                .mapToInt(Configuration::getId)
                .max()
                .orElse(0) + 1;
    }

    private void createParentDirectories(Path file) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
