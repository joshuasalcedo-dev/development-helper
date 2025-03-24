package com.joshuasalcedo.development.dependency;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * POJO representing a Maven dependency with additional metadata for tracking and analysis
 */

public class Dependency {
    // Basic Maven coordinates
    private String groupId;
    private String artifactId;
    private String version;
    private String scope;
    private String type;
    private String classifier;
    private boolean optional;

    // Additional metadata
    private String latestVersion;
    private List<String> availableVersions = new ArrayList<>();
    private boolean outdated = false;
    private boolean hasConflicts = false;
    private String conflictDetails;
    private boolean hasSecurityIssues = false;
    private String securityIssueDetails;
    private String repositoryUrl;
    private String localPath;
    private List<String> usedByClasses = new ArrayList<>();

    // Default constructor
    public Dependency () {
    }

    // Constructor with essential fields
    public Dependency (String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    // Constructor with all basic Maven coordinates
    public Dependency (String groupId, String artifactId, String version,
                       String scope, String type, String classifier, boolean optional) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.scope = scope;
        this.type = type;
        this.classifier = classifier;
        this.optional = optional;
    }

    // Get Maven coordinates in standard format
    public String getCoordinates () {
        StringBuilder sb = new StringBuilder();
        sb.append(groupId).append(":").append(artifactId).append(":").append(version);

        if (classifier != null && !classifier.isEmpty()) {
            sb.append(":").append(classifier);
        }

        if (type != null && !type.equals("jar")) {
            sb.append("@").append(type);
        }

        return sb.toString();
    }

    // Check if the dependency is outdated
    public boolean isOutdated () {
        return outdated || (latestVersion != null && !latestVersion.equals(version));
    }

    public void setOutdated (boolean outdated) {
        this.outdated = outdated;
    }

    public void addUsedByClass (String className) {
        if (!usedByClasses.contains(className)) {
            usedByClasses.add(className);
        }
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dependency that = (Dependency) o;
        return Objects.equals(groupId, that.groupId) &&
                Objects.equals(artifactId, that.artifactId) &&
                Objects.equals(version, that.version) &&
                Objects.equals(classifier, that.classifier);
    }

    @Override
    public int hashCode () {
        return Objects.hash(groupId, artifactId, version, classifier);
    }

    @Override
    public String toString () {
        return getCoordinates() + (isOutdated() ? " (OUTDATED: " + latestVersion + ")" : "");
    }

    public String getGroupId () {
        return groupId;
    }

    public void setGroupId (String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId () {
        return artifactId;
    }

    public void setArtifactId (String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion () {
        return version;
    }

    public void setVersion (String version) {
        this.version = version;
    }

    public String getScope () {
        return scope;
    }

    public void setScope (String scope) {
        this.scope = scope;
    }

    public String getType () {
        return type;
    }

    public void setType (String type) {
        this.type = type;
    }

    public String getClassifier () {
        return classifier;
    }

    public void setClassifier (String classifier) {
        this.classifier = classifier;
    }

    public boolean isOptional () {
        return optional;
    }

    public void setOptional (boolean optional) {
        this.optional = optional;
    }

    public String getLatestVersion () {
        return latestVersion;
    }

    public void setLatestVersion (String latestVersion) {
        this.latestVersion = latestVersion;
        this.outdated = latestVersion != null && !latestVersion.equals(version);
    }

    public List<String> getAvailableVersions () {
        return availableVersions;
    }

    public void setAvailableVersions (List<String> availableVersions) {
        this.availableVersions = availableVersions;
    }

    public boolean isHasConflicts () {
        return hasConflicts;
    }

    public void setHasConflicts (boolean hasConflicts) {
        this.hasConflicts = hasConflicts;
    }

    public String getConflictDetails () {
        return conflictDetails;
    }

    public void setConflictDetails (String conflictDetails) {
        this.conflictDetails = conflictDetails;
    }

    public boolean isHasSecurityIssues () {
        return hasSecurityIssues;
    }

    public void setHasSecurityIssues (boolean hasSecurityIssues) {
        this.hasSecurityIssues = hasSecurityIssues;
    }

    public String getSecurityIssueDetails () {
        return securityIssueDetails;
    }

    public void setSecurityIssueDetails (String securityIssueDetails) {
        this.securityIssueDetails = securityIssueDetails;
        this.hasSecurityIssues = securityIssueDetails != null && !securityIssueDetails.isEmpty();
    }

    public String getRepositoryUrl () {
        return repositoryUrl;
    }

    public void setRepositoryUrl (String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getLocalPath () {
        return localPath;
    }

    public void setLocalPath (String localPath) {
        this.localPath = localPath;
    }

    public List<String> getUsedByClasses () {
        return usedByClasses;
    }

    public void setUsedByClasses (List<String> usedByClasses) {
        this.usedByClasses = usedByClasses;
    }
}