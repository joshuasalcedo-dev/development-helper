package com.joshuasalcedo.development.dependency;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * POJO representing a Maven dependency with additional metadata for tracking and analysis
 */
@Getter
@Setter
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
    public Dependency() {
    }

    // Constructor with essential fields
    public Dependency(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    // Constructor with all basic Maven coordinates
    public Dependency(String groupId, String artifactId, String version,
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
    public String getCoordinates() {
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
    public boolean isOutdated() {
        return outdated || (latestVersion != null && !latestVersion.equals(version));
    }



    public void setLatestVersion(String latestVersion) {
        this.latestVersion = latestVersion;
        this.outdated = latestVersion != null && !latestVersion.equals(version);
    }


    public void setSecurityIssueDetails(String securityIssueDetails) {
        this.securityIssueDetails = securityIssueDetails;
        this.hasSecurityIssues = securityIssueDetails != null && !securityIssueDetails.isEmpty();
    }



    public void addUsedByClass(String className) {
        if (!usedByClasses.contains(className)) {
            usedByClasses.add(className);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dependency that = (Dependency) o;
        return Objects.equals(groupId, that.groupId) &&
                Objects.equals(artifactId, that.artifactId) &&
                Objects.equals(version, that.version) &&
                Objects.equals(classifier, that.classifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version, classifier);
    }

    @Override
    public String toString() {
        return getCoordinates() + (isOutdated() ? " (OUTDATED: " + latestVersion + ")" : "");
    }
}