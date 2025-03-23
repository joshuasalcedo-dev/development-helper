package com.joshuasalcedo.development.dependency;

import com.joshuasalcedo.development.module.ApplicationModule;

import java.io.File;
import java.util.List; /**
 * Example showing how to use the ApplicationModule annotation with the dependency management library
 */
@ApplicationModule(name = "dependencyAnalyzer",
        successMessage = "Dependency analysis completed successfully",
        errorMessage = "Dependency analysis failed")
public class DependencyAnalyzerModule {

    private final DependencyManager dependencyManager;

    public DependencyAnalyzerModule() {
        this.dependencyManager = new DependencyManager();
    }

    @ApplicationModule(name = "analyzeProject")
    public Project analyzeProject(String pomPath) throws Exception {
        File pomFile = new File(pomPath);
        return dependencyManager.analyzeProject(pomFile, true);
    }

    @ApplicationModule(name = "getOutdatedDependencies")
    public List<Dependency> getOutdatedDependencies(Project project) {
        return dependencyManager.getOutdatedDependencies(project);
    }

    @ApplicationModule(name = "getDependenciesWithIssues")
    public List<Dependency> getDependenciesWithIssues(Project project) {
        List<Dependency> result = dependencyManager.getDependenciesWithConflicts(project);
        result.addAll(dependencyManager.getDependenciesWithSecurityIssues(project));
        return result;
    }

    @ApplicationModule(name = "searchDependency")
    public List<Dependency> searchDependency(String groupId, String artifactId) throws Exception {
        return dependencyManager.searchDependencies(groupId, artifactId);
    }
}
