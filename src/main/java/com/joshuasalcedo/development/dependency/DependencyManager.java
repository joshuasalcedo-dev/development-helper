package com.joshuasalcedo.development.dependency;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture; /**
 * Main facade class for the dependency management library
 */
public class DependencyManager {
    private final DependencyService dependencyService;
    private final SecurityVulnerabilityService securityService;
    private final DependencyConflictService conflictService;

    public DependencyManager() {
        this.dependencyService = new DependencyService();
        this.securityService = new SecurityVulnerabilityService();
        this.conflictService = new DependencyConflictService();
    }

    /**
     * Parse a Maven POM file
     *
     * @param pomFile The POM file to parse
     * @return The parsed project
     */
    public Project parseProject(File pomFile) throws Exception {
        return dependencyService.parseProject(pomFile);
    }

    /**
     * Parse a Maven POM file and check for updates and conflicts
     *
     * @param pomFile The POM file to parse
     * @param checkSecurity Whether to check for security vulnerabilities
     * @return The parsed project with metadata populated
     */
    public Project analyzeProject(File pomFile, boolean checkSecurity) throws Exception {
        Project project = dependencyService.parseProject(pomFile);
        dependencyService.checkForUpdates(project);
        conflictService.detectConflicts(project);

        if (checkSecurity) {
            List<Dependency> allDependencies = dependencyService.getAllDependencies(project);
            securityService.checkVulnerabilities(allDependencies).join();
        }

        return project;
    }

    /**
     * Get all dependencies from a project
     *
     * @param project The project
     * @return List of all dependencies (regular and managed)
     */
    public List<Dependency> getAllDependencies(Project project) {
        return dependencyService.getAllDependencies(project);
    }

    /**
     * Get outdated dependencies from a project
     *
     * @param project The project
     * @return List of outdated dependencies
     */
    public List<Dependency> getOutdatedDependencies(Project project) {
        return dependencyService.getOutdatedDependencies(project);
    }

    /**
     * Get dependencies with conflicts from a project
     *
     * @param project The project
     * @return List of dependencies with conflicts
     */
    public List<Dependency> getDependenciesWithConflicts(Project project) {
        return dependencyService.getDependenciesWithConflicts(project);
    }

    /**
     * Get dependencies with security issues from a project
     *
     * @param project The project
     * @return List of dependencies with security issues
     */
    public List<Dependency> getDependenciesWithSecurityIssues(Project project) {
        return dependencyService.getDependenciesWithSecurityIssues(project);
    }

    /**
     * Search for dependencies by coordinates
     *
     * @param groupId The groupId to search for
     * @param artifactId The artifactId to search for
     * @return List of matching dependencies
     */
    public List<Dependency> searchDependencies(String groupId, String artifactId) throws Exception {
        return dependencyService.searchByCoordinates(groupId, artifactId);
    }

    /**
     * Get the latest version of a dependency
     *
     * @param groupId The groupId
     * @param artifactId The artifactId
     * @return The latest version, or null if not found
     */
    public String getLatestVersion(String groupId, String artifactId) throws Exception {
        return dependencyService.getLatestVersion(groupId, artifactId);
    }

    /**
     * Check a dependency for security vulnerabilities
     *
     * @param dependency The dependency to check
     * @return CompletableFuture of the dependency with security information populated
     */
    public CompletableFuture<Dependency> checkSecurity(Dependency dependency) {
        return securityService.checkVulnerabilities(dependency);
    }

    /**
     * Check dependencies for security vulnerabilities
     *
     * @param dependencies The dependencies to check
     * @return CompletableFuture of the dependencies with security information populated
     */
    public CompletableFuture<List<Dependency>> checkSecurity(List<Dependency> dependencies) {
        return securityService.checkVulnerabilities(dependencies);
    }
}
