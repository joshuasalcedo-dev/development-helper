package com.joshuasalcedo.development.dependency;

import org.apache.maven.api.model.Model;
import org.apache.maven.model.v4.MavenStaxReader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service class for handling dependency operations, including parsing projects,
 * checking for updates, and retrieving repository information
 */
public class DependencyService {
    private static final String MAVEN_CENTRAL_API = "https://search.maven.org/solrsearch/select";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final String LOCAL_REPO_PATH = getLocalRepositoryPath();

    private static final Map<String, Repository> knownRepositories = new ConcurrentHashMap<>();

    static {
        // Initialize known repositories
        knownRepositories.put("central", new Repository("central", "Maven Central", "https://repo.maven.apache.org/maven2/", "default", false));
        knownRepositories.put("google", new Repository("google", "Google Maven", "https://maven.google.com/", "default", false));
        knownRepositories.put("jcenter", new Repository("jcenter", "JCenter", "https://jcenter.bintray.com/", "default", false));
        knownRepositories.put("spring", new Repository("spring", "Spring Releases", "https://repo.spring.io/release/", "default", false));
        knownRepositories.put("local", new Repository("local", "Local Repository", LOCAL_REPO_PATH, "default", true));
    }

    /**
     * Get the local Maven repository path
     *
     * @return The path to the local Maven repository
     */
    private static String getLocalRepositoryPath () {
        // Try to get from settings.xml
        try {
            File settingsFile = new File(System.getProperty("user.home") + "/.m2/settings.xml");
            if (settingsFile.exists()) {
                String content = Files.readString(settingsFile.toPath());
                String localRepo = extractTag(content, "localRepository");
                if (localRepo != null && !localRepo.isEmpty()) {
                    return localRepo;
                }
            }

            // Try from M2_HOME if defined
            String m2Home = System.getenv("M2_HOME");
            if (m2Home != null) {
                File globalSettings = new File(m2Home, "conf/settings.xml");
                if (globalSettings.exists()) {
                    String content = Files.readString(globalSettings.toPath());
                    String localRepo = extractTag(content, "localRepository");
                    if (localRepo != null && !localRepo.isEmpty()) {
                        return localRepo;
                    }
                }
            }
        }
        catch (Exception e) {
            // Fall back to default
        }

        // Default user home .m2 directory
        return System.getProperty("user.home") + "/.m2/repository";
    }

    /**
     * Extract a tag value from XML content
     *
     * @param content The XML content
     * @param tagName The tag name to extract
     * @return The tag value, or null if not found
     */
    private static String extractTag (String content, String tagName) {
        int start = content.indexOf("<" + tagName + ">");
        if (start != -1) {
            start += tagName.length() + 2;
            int end = content.indexOf("</" + tagName + ">", start);
            if (end != -1) {
                return content.substring(start, end).trim();
            }
        }
        return null;
    }

    /**
     * Gets the path to a dependency in the local repository
     *
     * @param groupId    The groupId
     * @param artifactId The artifactId
     * @param version    The version
     * @return The path to the dependency in the local repository
     */
    private static String getLocalRepositoryPath (String groupId, String artifactId, String version) {
        String relativePath = groupId.replace('.', '/') +
                "/" + artifactId +
                "/" + version;
        return LOCAL_REPO_PATH + "/" + relativePath;
    }

    /**
     * Checks if a directory contains any jar files
     *
     * @param directory The directory to check
     * @return True if the directory contains any jar files, false otherwise
     */
    private static boolean hasAnyJar (File directory) {
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".jar"));
        return files != null && files.length > 0;
    }

    /**
     * Parse a Maven POM file and return a Project object
     *
     * @param pomFile The POM file to parse
     * @return Project object representing the Maven project
     * @throws Exception If the POM file cannot be read or parsed
     */
    public Project parseProject (File pomFile) throws Exception {
        MavenStaxReader reader = new MavenStaxReader();
        Model model;

        try (FileReader fileReader = new FileReader(pomFile)) {
            model = reader.read(fileReader);
        }

        Project project = new Project();
        project.setGroupId(model.getGroupId());
        project.setArtifactId(model.getArtifactId());
        project.setVersion(model.getVersion());
        project.setPackaging(model.getPackaging());
        project.setName(model.getName());
        project.setDescription(model.getDescription());
        project.setUrl(model.getUrl());
        project.setLocalPath(pomFile.getAbsolutePath());

        // Handle parent
        if (model.getParent() != null) {
            project.setParentCoordinates(
                    model.getParent().getGroupId() + ":" +
                            model.getParent().getArtifactId() + ":" +
                            model.getParent().getVersion()
            );
        }

        // Set properties
        if (model.getProperties() != null) {
            model.getProperties().forEach(project::addProperty);
        }

        // Set SCM URL
        if (model.getScm() != null && model.getScm().getUrl() != null) {
            project.setScmUrl(model.getScm().getUrl());
        }

        // Process repositories
        if (model.getRepositories() != null) {
            for (org.apache.maven.api.model.Repository repo : model.getRepositories()) {
                Repository repository = new Repository(
                        repo.getId(),
                        repo.getName(),
                        repo.getUrl(),
                        repo.getLayout(),
                        false
                );
                project.addRepository(repository);

                // Add to known repositories
                knownRepositories.putIfAbsent(repo.getId(), repository);
            }
        }

        // Add local repository if not already added
        if (project.getRepositories().stream().noneMatch(Repository::isLocal)) {
            project.addRepository(knownRepositories.get("local"));
        }

        // Process dependencies
        if (model.getDependencies() != null) {
            for (org.apache.maven.api.model.Dependency dep : model.getDependencies()) {
                Dependency dependency = new Dependency(
                        dep.getGroupId(),
                        dep.getArtifactId(),
                        dep.getVersion(),
                        dep.getScope(),
                        dep.getType(),
                        dep.getClassifier(),
                        dep.isOptional()
                );
                project.addDependency(dependency);
            }
        }

        // Process dependency management
        if (model.getDependencyManagement() != null && model.getDependencyManagement().getDependencies() != null) {
            for (org.apache.maven.api.model.Dependency dep : model.getDependencyManagement().getDependencies()) {
                Dependency dependency = new Dependency(
                        dep.getGroupId(),
                        dep.getArtifactId(),
                        dep.getVersion(),
                        dep.getScope(),
                        dep.getType(),
                        dep.getClassifier(),
                        dep.isOptional()
                );
                project.addManagedDependency(dependency);
            }
        }

        return project;
    }

    /**
     * Check for dependency updates and populate metadata
     *
     * @param project The project to check
     * @throws Exception If there's an error communicating with repositories
     */
    public void checkForUpdates (Project project) throws Exception {
        // Process regular dependencies
        for (Dependency dependency : project.getDependencies()) {
            enrichDependencyMetadata(dependency);
        }

        // Process managed dependencies
        for (Dependency dependency : project.getManagedDependencies()) {
            enrichDependencyMetadata(dependency);
        }
    }

    /**
     * Enrich a dependency with metadata from repositories
     *
     * @param dependency The dependency to enrich
     * @throws Exception If there's an error communicating with repositories
     */
    public void enrichDependencyMetadata (Dependency dependency) throws Exception {
        // Skip dependencies with property placeholders
        String version = dependency.getVersion();
        if (version != null && version.startsWith("${") && version.endsWith("}")) {
            return;
        }

        // Check local repository
        enrichWithLocalRepositoryInfo(dependency);

        // Check remote repositories
        enrichWithRemoteRepositoryInfo(dependency);
    }

    /**
     * Enrich dependency with local repository information
     *
     * @param dependency The dependency to enrich
     */
    private void enrichWithLocalRepositoryInfo (Dependency dependency) {
        List<String> localVersions = getLocalVersions(dependency.getGroupId(), dependency.getArtifactId());

        if (!localVersions.isEmpty()) {
            Collections.sort(localVersions);
            localVersions.forEach(dependency.getAvailableVersions()::add);

            String latestLocalVersion = localVersions.get(localVersions.size() - 1);

            // Set local path
            String path = getLocalRepositoryPath(
                    dependency.getGroupId(),
                    dependency.getArtifactId(),
                    dependency.getVersion()
            );
            dependency.setLocalPath(path);

            // Update latest version if needed
            if (dependency.getLatestVersion() == null ||
                    latestLocalVersion.compareTo(dependency.getLatestVersion()) > 0) {
                dependency.setLatestVersion(latestLocalVersion);
            }
        }
    }

    /**
     * Enrich dependency with remote repository information
     *
     * @param dependency The dependency to enrich
     * @throws Exception If there's an error communicating with repositories
     */
    private void enrichWithRemoteRepositoryInfo (Dependency dependency) throws Exception {
        List<Dependency> remoteResults = searchByCoordinates(
                dependency.getGroupId(),
                dependency.getArtifactId()
        );

        if (!remoteResults.isEmpty()) {
            Dependency remoteInfo = remoteResults.get(0);

            // Add available versions if not already present
            for (String version : remoteInfo.getAvailableVersions()) {
                if (!dependency.getAvailableVersions().contains(version)) {
                    dependency.getAvailableVersions().add(version);
                }
            }

            // Sort versions
            Collections.sort(dependency.getAvailableVersions());

            // Set latest version and repository URL
            if (remoteInfo.getLatestVersion() != null) {
                if (dependency.getLatestVersion() == null ||
                        remoteInfo.getLatestVersion().compareTo(dependency.getLatestVersion()) > 0) {
                    dependency.setLatestVersion(remoteInfo.getLatestVersion());
                }
            }

            if (remoteInfo.getRepositoryUrl() != null) {
                dependency.setRepositoryUrl(remoteInfo.getRepositoryUrl());
            } else {
                // Infer GitHub URL
                dependency.setRepositoryUrl(inferGithubUrl(
                        dependency.getGroupId(),
                        dependency.getArtifactId()
                ));
            }
        }
    }

    /**
     * Get a list of all dependencies from a project
     *
     * @param project The project
     * @return List of all dependencies (regular and managed)
     */
    public List<Dependency> getAllDependencies (Project project) {
        List<Dependency> allDependencies = new ArrayList<>(project.getDependencies());
        allDependencies.addAll(project.getManagedDependencies());
        return allDependencies;
    }

    /**
     * Get a list of outdated dependencies from a project
     *
     * @param project The project
     * @return List of outdated dependencies
     */
    public List<Dependency> getOutdatedDependencies (Project project) {
        return getAllDependencies(project).stream()
                .filter(Dependency::isOutdated)
                .collect(Collectors.toList());
    }

    /**
     * Get a list of dependencies with conflicts from a project
     *
     * @param project The project
     * @return List of dependencies with conflicts
     */
    public List<Dependency> getDependenciesWithConflicts (Project project) {
        return getAllDependencies(project).stream()
                .filter(Dependency::isHasConflicts)
                .collect(Collectors.toList());
    }

    /**
     * Get a list of dependencies with security issues from a project
     *
     * @param project The project
     * @return List of dependencies with security issues
     */
    public List<Dependency> getDependenciesWithSecurityIssues (Project project) {
        return getAllDependencies(project).stream()
                .filter(Dependency::isHasSecurityIssues)
                .collect(Collectors.toList());
    }

    /**
     * Search Maven repositories for artifacts matching specified coordinates
     *
     * @param groupId    The groupId to search for
     * @param artifactId The artifactId to search for
     * @return List of matching dependencies
     * @throws Exception If there's an error communicating with repositories
     */
    public List<Dependency> searchByCoordinates (String groupId, String artifactId) throws Exception {
        String query = "g:" + groupId + " AND a:" + artifactId;
        return searchArtifacts(query, 20);
    }

    /**
     * Search Maven repositories for artifacts matching a query
     *
     * @param query The search query
     * @param rows  The maximum number of results to return
     * @return List of matching dependencies
     * @throws Exception If there's an error communicating with repositories
     */
    public List<Dependency> searchArtifacts (String query, int rows) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = MAVEN_CENTRAL_API + "?q=" + encodedQuery + "&rows=" + rows + "&wt=json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return parseSearchResults(response.body());
    }

    /**
     * Search Maven repositories asynchronously
     *
     * @param query The search query
     * @param rows  The maximum number of results to return
     * @return CompletableFuture of list of matching dependencies
     */
    public CompletableFuture<List<Dependency>> searchArtifactsAsync (String query, int rows) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = MAVEN_CENTRAL_API + "?q=" + encodedQuery + "&rows=" + rows + "&wt=json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(this::parseSearchResults);
    }

    /**
     * Get the latest version of a dependency
     *
     * @param groupId    The groupId
     * @param artifactId The artifactId
     * @return The latest version, or null if not found
     * @throws Exception If there's an error communicating with repositories
     */
    public String getLatestVersion (String groupId, String artifactId) throws Exception {
        List<Dependency> results = searchByCoordinates(groupId, artifactId);
        return results.isEmpty() ? null : results.get(0).getLatestVersion();
    }

    /**
     * Parse search results from Maven repositories
     *
     * @param json The JSON response from the repository
     * @return List of dependencies from the search results
     */
    private List<Dependency> parseSearchResults (String json) {
        List<Dependency> results = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONObject response = jsonObject.getJSONObject("response");
            JSONArray docs = response.getJSONArray("docs");

            for (int i = 0; i < docs.length(); i++) {
                JSONObject doc = docs.getJSONObject(i);

                String groupId = doc.getString("g");
                String artifactId = doc.getString("a");
                String latestVersion = doc.optString("latestVersion", "");

                Dependency dependency = new Dependency(groupId, artifactId, null);
                dependency.setLatestVersion(latestVersion);

                // Extract versions if available
                if (doc.has("v")) {
                    JSONArray vArray = doc.getJSONArray("v");
                    for (int j = 0; j < vArray.length(); j++) {
                        dependency.getAvailableVersions().add(vArray.getString(j));
                    }
                }

                // Try to get the SCM URL if available
                if (doc.has("scm")) {
                    dependency.setRepositoryUrl(doc.getString("scm"));
                }

                results.add(dependency);
            }
        }
        catch (Exception e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
        }

        return results;
    }

    /**
     * Gets available versions of a dependency in the local repository
     *
     * @param groupId    The groupId
     * @param artifactId The artifactId
     * @return List of available versions in the local repository
     */
    private List<String> getLocalVersions (String groupId, String artifactId) {
        List<String> versions = new ArrayList<>();

        // Construct the path to the dependency directory
        String basePath = LOCAL_REPO_PATH + "/" + groupId.replace('.', '/') + "/" + artifactId;
        File baseDir = new File(basePath);

        if (baseDir.exists() && baseDir.isDirectory()) {
            // Each subdirectory is a version
            File[] versionDirs = baseDir.listFiles(File::isDirectory);
            if (versionDirs != null) {
                for (File versionDir : versionDirs) {
                    // Check if the jar file exists (to ensure it's a valid version)
                    File jarFile = new File(versionDir, artifactId + "-" + versionDir.getName() + ".jar");
                    if (jarFile.exists() || hasAnyJar(versionDir)) {
                        versions.add(versionDir.getName());
                    }
                }
            }
        }

        return versions;
    }

    /**
     * Infer a GitHub URL for a dependency
     *
     * @param groupId    The groupId
     * @param artifactId The artifactId
     * @return The inferred GitHub URL, or null if it cannot be inferred
     */
    private String inferGithubUrl (String groupId, String artifactId) {
        // Try to derive GitHub URL from groupId
        String[] groupParts = groupId.split("\\.");
        if (groupParts.length >= 2) {
            String organization = groupParts[1];
            if (groupParts[0].equals("com") || groupParts[0].equals("org") || groupParts[0].equals("io")) {
                if (groupParts[1].equals("github")) {
                    // Handle com.github.user format
                    if (groupParts.length >= 3) {
                        return "https://github.com/" + groupParts[2] + "/" + artifactId;
                    }
                } else {
                    // Handle common formats like com.organization.project
                    return "https://github.com/" + organization + "/" + artifactId;
                }
            }
        }

        // If all else fails, return a search URL
        return "https://github.com/search?q=" + groupId + "+" + artifactId;
    }
}