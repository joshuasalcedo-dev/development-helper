package com.joshuasalcedo.development.dependency;

import java.util.*;
import java.util.stream.Collectors; /**
 * Service for detecting dependency conflicts in a project
 */
public class DependencyConflictService {

    /**
     * Detect version conflicts in a project's dependencies
     *
     * @param project The project to check
     * @return The project with conflict information populated
     */
    public Project detectConflicts(Project project) {
        Map<String, List<Dependency>> dependenciesByKey = new HashMap<>();

        // Group dependencies by groupId:artifactId
        for (Dependency dependency : project.getDependencies()) {
            String key = dependency.getGroupId() + ":" + dependency.getArtifactId();
            dependenciesByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(dependency);
        }

        // Check for conflicts (same groupId:artifactId with different versions)
        for (Map.Entry<String, List<Dependency>> entry : dependenciesByKey.entrySet()) {
            List<Dependency> deps = entry.getValue();

            if (deps.size() > 1) {
                // Check if versions differ
                Set<String> versions = deps.stream()
                        .map(Dependency::getVersion)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                if (versions.size() > 1) {
                    // We have a conflict
                    String details = "Multiple versions found: " + String.join(", ", versions);

                    // Set conflict details on all affected dependencies
                    for (Dependency dep : deps) {
                        dep.setConflictDetails(details);
                        dep.setHasConflicts(true);
                    }
                }
            }
        }

        return project;
    }

    /**
     * Detect transitive dependency conflicts in a project
     * This requires the project's dependency tree to be loaded
     *
     * @param project The project to check
     * @param dependencyTree The dependency tree (as from 'mvn dependency:tree')
     * @return The project with conflict information populated
     */
    public Project detectTransitiveConflicts(Project project, String dependencyTree) {
        // Parse dependency tree output to find conflicts
        // Example line with conflict: "[INFO] |  \\- commons-collections:commons-collections:jar:3.2.1:compile (version managed from 3.1)"

        Map<String, Dependency> dependencyMap = new HashMap<>();

        // Build a map of all dependencies
        for (Dependency dep : project.getDependencies()) {
            String key = dep.getGroupId() + ":" + dep.getArtifactId();
            dependencyMap.put(key, dep);
        }

        // Parse the dependency tree
        String[] lines = dependencyTree.split("\\n");
        for (String line : lines) {
            if (line.contains("(version managed from ")) {
                try {
                    // Extract the dependency information
                    int startIdx = line.indexOf("- ") + 2;
                    int endIdx = line.indexOf(":", startIdx + 1);
                    String groupId = line.substring(startIdx, endIdx);

                    startIdx = endIdx + 1;
                    endIdx = line.indexOf(":", startIdx);
                    String artifactId = line.substring(startIdx, endIdx);

                    // Extract the conflict information
                    startIdx = line.indexOf("(version managed from ") + "(version managed from ".length();
                    endIdx = line.indexOf(")", startIdx);
                    String conflictVersion = line.substring(startIdx, endIdx);

                    // Find the resolved version
                    startIdx = line.indexOf(":", line.indexOf(":", line.indexOf(":", endIdx - 20) + 1) + 1) + 1;
                    endIdx = line.indexOf(":", startIdx);
                    String resolvedVersion = line.substring(startIdx, endIdx);

                    // Set conflict details
                    String key = groupId + ":" + artifactId;
                    Dependency dep = dependencyMap.get(key);

                    if (dep != null) {
                        String details = "Version conflict: " + conflictVersion + " -> " + resolvedVersion;
                        dep.setConflictDetails(details);
                        dep.setHasConflicts(true);
                    }
                } catch (Exception e) {
                    // Skip this line if we can't parse it
                    System.err.println("Error parsing dependency tree line: " + line);
                }
            }
        }

        return project;
    }
}
