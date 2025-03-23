package com.joshuasalcedo.development.dependency;

import com.joshuasalcedo.development.dependency.Dependency;
import com.joshuasalcedo.development.dependency.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * POJO representing a Maven project with its dependencies and metadata
 */
public class Project {
    private String groupId;
    private String artifactId;
    private String version;
    private String packaging;
    private String name;
    private String description;
    private String url;

    private List<Dependency> dependencies = new ArrayList<>();
    private List<Dependency> managedDependencies = new ArrayList<>();
    private Map<String, String> properties = new HashMap<>();
    private List<Repository> repositories = new ArrayList<>();

    private String scmUrl;
    private String localPath;
    private String parentCoordinates;

    // Default constructor
    public Project() {
    }

    // Constructor with essential fields
    public Project(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    // Get project coordinates in standard format
    public String getCoordinates() {
        return groupId + ":" + artifactId + ":" + version;
    }

    // Add a dependency
    public void addDependency(Dependency dependency) {
        this.dependencies.add(dependency);
    }

    // Add a managed dependency
    public void addManagedDependency(Dependency dependency) {
        this.managedDependencies.add(dependency);
    }

    // Add a repository
    public void addRepository(Repository repository) {
        this.repositories.add(repository);
    }

    // Add a property
    public void addProperty(String key, String value) {
        this.properties.put(key, value);
    }

    // Getters and Setters
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPackaging() {
        return packaging;
    }

    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    public List<Dependency> getManagedDependencies() {
        return managedDependencies;
    }

    public void setManagedDependencies(List<Dependency> managedDependencies) {
        this.managedDependencies = managedDependencies;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public List<Repository> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<Repository> repositories) {
        this.repositories = repositories;
    }

    public String getScmUrl() {
        return scmUrl;
    }

    public void setScmUrl(String scmUrl) {
        this.scmUrl = scmUrl;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getParentCoordinates() {
        return parentCoordinates;
    }

    public void setParentCoordinates(String parentCoordinates) {
        this.parentCoordinates = parentCoordinates;
    }

    @Override
    public String toString() {
        return getName() != null ? getName() : getCoordinates();
    }
}