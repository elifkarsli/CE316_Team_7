package com.iae.service;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.iae.dao.ConfigurationDAO;
import com.iae.dao.ProjectDAO;
import com.iae.model.Configuration;

public class ConfigurationService {
    private static final Type CONFIGURATION_LIST_TYPE = new TypeToken<List<Configuration>>() {
    }.getType();
    private static final String CONFIGURATION_IN_USE_MESSAGE =
            "This configuration is currently used by a project and cannot be deleted.";

    private final ProjectDAO       projectDAO = new ProjectDAO();
    private final ConfigurationDAO configDAO  = new ConfigurationDAO();
    private final Path storageFile;
    private final Path projectStorageFile;
    private final Gson gson;
    public ConfigurationService() {
        this(getAppDataDirectory().resolve("configurations.json"), 
     getAppDataDirectory().resolve("projects.json"));
    }

    public ConfigurationService(Path storageFile) {
        this(storageFile, Path.of("data", "projects.json"));
    }

    public ConfigurationService(Path storageFile, Path projectStorageFile) {
        this.storageFile = storageFile;
        this.projectStorageFile = projectStorageFile;
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
    private static Path getAppDataDirectory() {
    String os = System.getProperty("os.name").toLowerCase();
    Path dataDir;
    if (os.contains("win")) {
        dataDir = Paths.get(System.getenv("APPDATA"), "IAE", "data");
    } else if (os.contains("mac")) {
        dataDir = Paths.get(System.getProperty("user.home"), 
            "Library", "Application Support", "IAE", "data");
    } else {
        dataDir = Paths.get(System.getProperty("user.home"), ".iae", "data");
    }
    try {
        Files.createDirectories(dataDir);
    } catch (IOException e) {
        throw new RuntimeException("Cannot create data directory: " + e.getMessage());
    }
    return dataDir;
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
        boolean exists = configurations.stream()
                .anyMatch(configuration -> configuration.getId() == id);

        if (!exists) {
            return false;
        }

        if (isConfigurationUsedByProject(id)) {
            throw new IllegalStateException(CONFIGURATION_IN_USE_MESSAGE);
        }

        configurations.removeIf(configuration -> configuration.getId() == id);
        writeConfigurations(configurations);
        return true;
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
        Set<String> existingNames = configurationNames(configurations);

        for (Configuration importedConfiguration : importedConfigurations) {
            validateConfiguration(importedConfiguration);
        }

        int nextId = nextId(configurations);
        for (Configuration importedConfiguration : importedConfigurations) {
            String normalizedName = normalizeName(importedConfiguration.getName());
            if (existingNames.contains(normalizedName)) {
                continue;
            }

            importedConfiguration.setId(nextId++);
            configurations.add(importedConfiguration);
            savedConfigurations.add(importedConfiguration);
            existingNames.add(normalizedName);
        }

        if (!savedConfigurations.isEmpty()) {
            writeConfigurations(configurations);
        }

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
            JsonElement importedJson = gson.fromJson(reader, JsonElement.class);
            if (importedJson == null || importedJson.isJsonNull()) {
                throw new IOException("Import file does not contain a configuration.");
            }

            if (importedJson.isJsonObject()) {
                Configuration configuration = gson.fromJson(importedJson, Configuration.class);
                return new ArrayList<>(List.of(configuration));
            }

            if (importedJson.isJsonArray()) {
                List<Configuration> configurations = gson.fromJson(importedJson, CONFIGURATION_LIST_TYPE);
                return configurations == null ? new ArrayList<>() : configurations;
            }

            throw new IOException("Import file must contain a configuration object or a list of configurations.");
        } catch (JsonParseException exception) {
            throw new IOException("Could not import configuration JSON file.", exception);
        }
    }

    private boolean isConfigurationUsedByProject(int configurationId) throws IOException {
        if (Files.notExists(projectStorageFile)) {
            return false;
        }

        try (Reader reader = Files.newBufferedReader(projectStorageFile)) {
            JsonElement projectsJson = gson.fromJson(reader, JsonElement.class);
            return containsConfigurationReference(projectsJson, configurationId);
        } catch (JsonParseException exception) {
            throw new IOException("Could not read projects JSON file.", exception);
        }
    }

    private boolean containsConfigurationReference(JsonElement element, int configurationId) {
        if (element == null || element.isJsonNull()) {
            return false;
        }

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                if (containsConfigurationReference(item, configurationId)) {
                    return true;
                }
            }
            return false;
        }

        if (!element.isJsonObject()) {
            return false;
        }

        JsonObject object = element.getAsJsonObject();
        if (hasMatchingId(object, "configurationId", configurationId)
                || hasMatchingId(object, "configId", configurationId)
                || hasMatchingConfigurationObject(object, configurationId)) {
            return true;
        }

        for (JsonElement child : object.asMap().values()) {
            if (containsConfigurationReference(child, configurationId)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasMatchingConfigurationObject(JsonObject object, int configurationId) {
        JsonElement configurationElement = object.get("configuration");
        if (configurationElement == null || configurationElement.isJsonNull()) {
            return false;
        }

        if (isMatchingNumber(configurationElement, configurationId)) {
            return true;
        }

        if (!configurationElement.isJsonObject()) {
            return false;
        }

        return hasMatchingId(configurationElement.getAsJsonObject(), "id", configurationId);
    }

    private boolean hasMatchingId(JsonObject object, String memberName, int expectedId) {
        JsonElement idElement = object.get(memberName);
        return isMatchingNumber(idElement, expectedId);
    }

    private boolean isMatchingNumber(JsonElement element, int expectedValue) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return false;
        }

        try {
            return element.getAsInt() == expectedValue;
        } catch (NumberFormatException exception) {
            return false;
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

    private Set<String> configurationNames(List<Configuration> configurations) {
        Set<String> names = new HashSet<>();
        for (Configuration configuration : configurations) {
            String normalizedName = normalizeName(configuration.getName());
            if (normalizedName != null) {
                names.add(normalizedName);
            }
        }
        return names;
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

    private String normalizeName(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }
}
