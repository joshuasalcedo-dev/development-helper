package com.joshuasalcedo.development;



import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Repository;
import org.apache.maven.model.v4.MavenStaxReader;

import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;


public class MultiRepoDependencyChecker {
    private static final String MAVEN_CENTRAL_API = "https://search.maven.org/solrsearch/select";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Get the local repository path from Maven settings or use default
    private static final String LOCAL_REPO_PATH = getLocalRepositoryPath();

    // List of remote repositories to check
    private static final List<RemoteRepository> REMOTE_REPOSITORIES = new ArrayList<>();

    public static void main(String[] args) {
        try {
            initializeRepositories();

            // Path to your project's pom.xml file
            String pomPath = "pom.xml";

            // Create a reader instance
            MavenStaxReader reader = new MavenStaxReader();

            // Load the POM file
            try (FileReader fileReader = new FileReader(pomPath)) {
                Model model = reader.read(fileReader);

                System.out.println("Project: " + model.getArtifactId() + " " + model.getVersion());
                System.out.println("Local repository: " + LOCAL_REPO_PATH);
                System.out.println("Remote repositories: " + REMOTE_REPOSITORIES.size());
                for (RemoteRepository repo : REMOTE_REPOSITORIES) {
                    System.out.println(" - " + repo.getName() + ": " + repo.getUrl());
                }

                System.out.println("\nChecking dependencies across all repositories...\n");

                // Check each dependency
                for (Dependency dependency : model.getDependencies()) {
                    checkDependencyAcrossRepositories(dependency);
                }

                // Also check for repository definitions in the POM itself
                List<Repository> pomRepositories = model.getRepositories();
                if (pomRepositories != null && !pomRepositories.isEmpty()) {
                    System.out.println("\nNote: This POM defines " + pomRepositories.size() +
                            " additional repositories that might contain dependencies:");
                    for (Repository repo : pomRepositories) {
                        System.out.println(" - " + repo.getId() + ": " + repo.getUrl());
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initialize the list of repositories to check.
     * This includes Maven Central, Google's Maven repo, JCenter, and others.
     */
    private static void initializeRepositories() {
        // Default repositories
        REMOTE_REPOSITORIES.add(new RemoteRepository("central", "Maven Central",
                "https://repo.maven.apache.org/maven2/"));
        REMOTE_REPOSITORIES.add(new RemoteRepository("google", "Google Maven",
                "https://maven.google.com/"));
        REMOTE_REPOSITORIES.add(new RemoteRepository("jcenter", "JCenter",
                "https://jcenter.bintray.com/"));
        REMOTE_REPOSITORIES.add(new RemoteRepository("spring", "Spring Releases",
                "https://repo.spring.io/release/"));
        REMOTE_REPOSITORIES.add(new RemoteRepository("atlassian", "Atlassian Public",
                "https://packages.atlassian.com/maven-external/"));

        // Try to read additional repositories from settings.xml
        try {
            // Check user settings first
            File userSettings = new File(System.getProperty("user.home") + "/.m2/settings.xml");
            if (userSettings.exists()) {
                String content = Files.readString(userSettings.toPath());
                addRepositoriesFromSettings(content);
            }

            // Then check global settings if M2_HOME is defined
            String m2Home = System.getenv("M2_HOME");
            if (m2Home != null) {
                File globalSettings = new File(m2Home, "conf/settings.xml");
                if (globalSettings.exists()) {
                    String content = Files.readString(globalSettings.toPath());
                    addRepositoriesFromSettings(content);
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Error reading Maven settings: " + e.getMessage());
        }
    }

    /**
     * Extract repository definitions from settings.xml content
     */
    private static void addRepositoriesFromSettings(String content) {
        try {
            int startProfiles = content.indexOf("<profiles>");
            if (startProfiles != -1) {
                int endProfiles = content.indexOf("</profiles>", startProfiles);
                if (endProfiles != -1) {
                    String profilesContent = content.substring(startProfiles, endProfiles);

                    // Find all repositories
                    int startPos = 0;
                    while (true) {
                        int repoStart = profilesContent.indexOf("<repository>", startPos);
                        if (repoStart == -1) break;

                        int repoEnd = profilesContent.indexOf("</repository>", repoStart);
                        if (repoEnd == -1) break;

                        String repoContent = profilesContent.substring(repoStart, repoEnd + "</repository>".length());
                        startPos = repoEnd + "</repository>".length();

                        // Extract repository details
                        String id = extractTag(repoContent, "id");
                        String name = extractTag(repoContent, "name");
                        String url = extractTag(repoContent, "url");

                        if (id != null && url != null) {
                            if (name == null) name = id;

                            // Check if it's already in our list
                            boolean exists = REMOTE_REPOSITORIES.stream()
                                    .anyMatch(repo -> repo.getId().equals(id) || repo.getUrl().equals(url));

                            if (!exists) {
                                REMOTE_REPOSITORIES.add(new RemoteRepository(id, name, url));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Error parsing repositories from settings: " + e.getMessage());
        }
    }

    /**
     * Extract a tag value from XML content
     */
    private static String extractTag(String content, String tagName) {
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
     * Gets the local Maven repository path from settings.xml or uses default
     */
    private static String getLocalRepositoryPath() {
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
        } catch (Exception e) {
            // Fall back to default
        }

        // Default user home .m2 directory
        return System.getProperty("user.home") + "/.m2/repository";
    }

    /**
     * Checks a dependency across all repositories
     */
    private static void checkDependencyAcrossRepositories(Dependency dependency) {
        String groupId = dependency.getGroupId();
        String artifactId = dependency.getArtifactId();
        String currentVersion = dependency.getVersion();

        // Handle property placeholders in version
        if (currentVersion != null && currentVersion.startsWith("${") && currentVersion.endsWith("}")) {
            System.out.println("Skipping " + groupId + ":" + artifactId +
                    " - Version uses property placeholder: " + currentVersion);
            return;
        }

        System.out.println("Checking " + groupId + ":" + artifactId + ":" + currentVersion);

        // Map to store versions found in each repository
        Map<String, List<String>> versionsMap = new HashMap<>();
        Map<String, String> latestVersions = new HashMap<>();

        // Check local repository first
        List<String> localVersions = getLocalVersions(groupId, artifactId);
        if (!localVersions.isEmpty()) {
            Collections.sort(localVersions);
            String latestLocalVersion = localVersions.get(localVersions.size() - 1);

            versionsMap.put("local", localVersions);
            latestVersions.put("local", latestLocalVersion);
        }

        // Check remote repositories in parallel
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(REMOTE_REPOSITORIES.size(), 10));
        List<Future<RepositoryResult>> futures = new ArrayList<>();

        // Submit tasks for each repository
        for (RemoteRepository repo : REMOTE_REPOSITORIES) {
            Future<RepositoryResult> future = executor.submit(() ->
                    checkRepositoryForDependency(repo, groupId, artifactId));
            futures.add(future);
        }

        // Collect results
        for (int i = 0; i < futures.size(); i++) {
            try {
                RepositoryResult result = futures.get(i).get(15, TimeUnit.SECONDS);
                if (result != null && !result.getVersions().isEmpty()) {
                    versionsMap.put(result.getRepositoryId(), result.getVersions());
                    latestVersions.put(result.getRepositoryId(), result.getLatestVersion());
                }
            } catch (Exception e) {
                // Timeout or error for this repository, continue with others
            }
        }

        executor.shutdown();

        // Print results
        System.out.println("  Current version: " + currentVersion);

        if (versionsMap.isEmpty()) {
            System.out.println("  No versions found in any repository");
        } else {
            // Find global latest version
            String globalLatest = latestVersions.values().stream()
                    .max(String::compareTo)
                    .orElse("");

            if (!globalLatest.isEmpty() && (currentVersion == null ||
                    globalLatest.compareTo(currentVersion) > 0)) {
                System.out.println("  UPGRADE AVAILABLE: " + globalLatest);
            }

            // Print versions for each repository
            for (Map.Entry<String, List<String>> entry : versionsMap.entrySet()) {
                String repoId = entry.getKey();
                List<String> versions = entry.getValue();

                // Show only most recent 5 versions if there are many
                List<String> displayVersions = versions;
                if (versions.size() > 5) {
                    displayVersions = versions.subList(versions.size() - 5, versions.size());
                }

                if (repoId.equals("local")) {
                    System.out.println("  Local repository (" + versions.size() + " versions):");
                    System.out.println("    Latest: " + latestVersions.get(repoId));
                    System.out.println("    Recent: " + String.join(", ", displayVersions));
                    System.out.println("    Path: " + getLocalRepositoryPath(groupId, artifactId,
                            latestVersions.get(repoId)));
                } else {
                    // Find matching remote repository
                    Optional<RemoteRepository> repo = REMOTE_REPOSITORIES.stream()
                            .filter(r -> r.getId().equals(repoId))
                            .findFirst();

                    if (repo.isPresent()) {
                        System.out.println("  " + repo.get().getName() + " (" + versions.size() + " versions):");
                        System.out.println("    Latest: " + latestVersions.get(repoId));
                        System.out.println("    Recent: " + String.join(", ", displayVersions));
                        System.out.println("    URL: " + constructArtifactUrl(repo.get(), groupId, artifactId,
                                latestVersions.get(repoId)));
                    }
                }
            }
        }

        System.out.println();
    }

    /**
     * Constructs the URL to an artifact in a remote repository
     */
    private static String constructArtifactUrl(RemoteRepository repo, String groupId,
                                               String artifactId, String version) {
        String baseUrl = repo.getUrl();
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        return baseUrl + groupId.replace('.', '/') + "/" +
                artifactId + "/" + version;
    }

    /**
     * Checks a single remote repository for a dependency
     */
    private static RepositoryResult checkRepositoryForDependency(
            RemoteRepository repo, String groupId, String artifactId) {
        try {
            String url = constructMetadataUrl(repo, groupId, artifactId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String metadata = response.body();
                List<String> versions = extractVersionsFromMetadata(metadata);

                if (!versions.isEmpty()) {
                    Collections.sort(versions);
                    return new RepositoryResult(
                            repo.getId(),
                            versions,
                            versions.get(versions.size() - 1)
                    );
                }
            }

            // Try Maven-style directory listing as a fallback
            return checkRepositoryByDirectoryListing(repo, groupId, artifactId);

        } catch (Exception e) {
            // Repository not available or error occurred
            return null;
        }
    }

    /**
     * Try to get versions by parsing directory listings (works for some repositories)
     */
    private static RepositoryResult checkRepositoryByDirectoryListing(
            RemoteRepository repo, String groupId, String artifactId) {
        try {
            String url = repo.getUrl();
            if (!url.endsWith("/")) url += "/";
            url += groupId.replace('.', '/') + "/" + artifactId + "/";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String listing = response.body();
                List<String> versions = new ArrayList<>();

                // Simple regex to find version directories
                // This is a heuristic and may not work on all repository servers
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                        "href=\"([0-9][^\"]*/)\"");
                java.util.regex.Matcher matcher = pattern.matcher(listing);

                while (matcher.find()) {
                    String version = matcher.group(1);
                    if (version.endsWith("/")) {
                        version = version.substring(0, version.length() - 1);
                    }
                    versions.add(version);
                }

                if (!versions.isEmpty()) {
                    Collections.sort(versions);
                    return new RepositoryResult(
                            repo.getId(),
                            versions,
                            versions.get(versions.size() - 1)
                    );
                }
            }

            return null;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Construct the URL to the maven-metadata.xml file
     */
    private static String constructMetadataUrl(RemoteRepository repo, String groupId, String artifactId) {
        String baseUrl = repo.getUrl();
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        return baseUrl + groupId.replace('.', '/') + "/" +
                artifactId + "/maven-metadata.xml";
    }

    /**
     * Extract version information from maven-metadata.xml
     */
    private static List<String> extractVersionsFromMetadata(String metadata) {
        List<String> versions = new ArrayList<>();

        try {
            // Find <versions> tag
            int versionsStart = metadata.indexOf("<versions>");
            if (versionsStart != -1) {
                int versionsEnd = metadata.indexOf("</versions>", versionsStart);
                if (versionsEnd != -1) {
                    String versionsContent = metadata.substring(versionsStart, versionsEnd);

                    // Extract each version
                    int startPos = 0;
                    while (true) {
                        int versionStart = versionsContent.indexOf("<version>", startPos);
                        if (versionStart == -1) break;

                        versionStart += "<version>".length();
                        int versionEnd = versionsContent.indexOf("</version>", versionStart);
                        if (versionEnd == -1) break;

                        String version = versionsContent.substring(versionStart, versionEnd);
                        versions.add(version);
                        startPos = versionEnd + "</version>".length();
                    }
                }
            }

            // If no versions found but there's a release version, use that
            if (versions.isEmpty()) {
                String releaseVersion = extractTag(metadata, "release");
                if (releaseVersion != null) {
                    versions.add(releaseVersion);
                }

                String latestVersion = extractTag(metadata, "latest");
                if (latestVersion != null && !versions.contains(latestVersion)) {
                    versions.add(latestVersion);
                }
            }
        } catch (Exception e) {
            // Return empty list if parsing fails
        }

        return versions;
    }

    /**
     * Gets the path to a dependency in the local repository
     */
    private static String getLocalRepositoryPath(String groupId, String artifactId, String version) {
        String relativePath = groupId.replace('.', '/') +
                "/" + artifactId +
                "/" + version;
        return LOCAL_REPO_PATH + "/" + relativePath;
    }

    /**
     * Gets available versions of a dependency in the local repository
     */
    private static List<String> getLocalVersions(String groupId, String artifactId) {
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
     * Checks if a directory contains any jar files (for classifiers and other variants)
     */
    private static boolean hasAnyJar(File directory) {
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".jar"));
        return files != null && files.length > 0;
    }

    /**
     * Class representing a remote Maven repository
     */
    private static class RemoteRepository {
        private final String id;
        private final String name;
        private final String url;

        public RemoteRepository(String id, String name, String url) {
            this.id = id;
            this.name = name;
            this.url = url;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }
    }

    /**
     * Class to hold the result of checking a repository
     */
    private static class RepositoryResult {
        private final String repositoryId;
        private final List<String> versions;
        private final String latestVersion;

        public RepositoryResult(String repositoryId, List<String> versions, String latestVersion) {
            this.repositoryId = repositoryId;
            this.versions = versions;
            this.latestVersion = latestVersion;
        }

        public String getRepositoryId() {
            return repositoryId;
        }

        public List<String> getVersions() {
            return versions;
        }

        public String getLatestVersion() {
            return latestVersion;
        }
    }
}