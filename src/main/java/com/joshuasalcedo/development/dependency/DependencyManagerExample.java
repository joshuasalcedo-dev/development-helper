package com.joshuasalcedo.development.dependency;

import java.io.File;
import java.util.List;

/**
 * Example showing how to use the dependency management library
 */
public class DependencyManagerExample {

    public static void main (String[] args) {
        try {
            // Create the dependency manager
            DependencyManager manager = new DependencyManager();

            // Parse and analyze a project
            File pomFile = new File("pom.xml");
            Project project = manager.analyzeProject(pomFile, true);

            // Print project information
            System.out.println("Project: " + project.getCoordinates());
            System.out.println("Name: " + project.getName());
            System.out.println("Description: " + project.getDescription());

            // Print dependencies
            System.out.println("\nDependencies: " + project.getDependencies().size());
            for (Dependency dependency : project.getDependencies()) {
                System.out.println(" - " + dependency.getCoordinates() +
                        (dependency.isOutdated() ? " (OUTDATED: " + dependency.getLatestVersion() + ")" : "") +
                        (dependency.isHasConflicts() ? " (CONFLICT)" : "") +
                        (dependency.isHasSecurityIssues() ? " (SECURITY ISSUE)" : ""));
            }

            // Print outdated dependencies
            List<Dependency> outdated = manager.getOutdatedDependencies(project);
            System.out.println("\nOutdated dependencies: " + outdated.size());
            for (Dependency dependency : outdated) {
                System.out.println(" - " + dependency.getCoordinates() +
                        " -> " + dependency.getLatestVersion());
            }

            // Print dependencies with conflicts
            List<Dependency> conflicts = manager.getDependenciesWithConflicts(project);
            System.out.println("\nDependencies with conflicts: " + conflicts.size());
            for (Dependency dependency : conflicts) {
                System.out.println(" - " + dependency.getCoordinates() +
                        ": " + dependency.getConflictDetails());
            }

            // Print dependencies with security issues
            List<Dependency> security = manager.getDependenciesWithSecurityIssues(project);
            System.out.println("\nDependencies with security issues: " + security.size());
            for (Dependency dependency : security) {
                System.out.println(" - " + dependency.getCoordinates());
                System.out.println("   " + dependency.getSecurityIssueDetails().replace("\n", "\n   "));
            }

            // Search for a dependency
            System.out.println("\nSearching for Spring Boot dependencies...");
            List<Dependency> results = manager.searchDependencies("org.springframework.boot", "spring-boot-starter");
            for (Dependency dependency : results) {
                System.out.println(" - " + dependency.getGroupId() + ":" + dependency.getArtifactId() +
                        " (Latest: " + dependency.getLatestVersion() + ")");
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
