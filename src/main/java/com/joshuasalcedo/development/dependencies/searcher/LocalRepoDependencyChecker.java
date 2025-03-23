package com.joshuasalcedo.development.dependencies.searcher;



import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Model;
import org.apache.maven.model.v4.MavenStaxReader;

import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Utility to check for available upgrades in both local and remote Maven repositories
 */
public class LocalRepoDependencyChecker {
    private static final String MAVEN_CENTRAL_API = "https://search.maven.org/solrsearch/select";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    // Get the local repository path from Maven settings or use default
    private static final String LOCAL_REPO_PATH = getLocalRepositoryPath();

    public static void main(String[] args) {
        try {
            // Path to your project's pom.xml file
            String pomPath = "pom.xml";

            // Create a reader instance
            MavenStaxReader reader = new MavenStaxReader();

            // Load the POM file
            try (FileReader fileReader = new FileReader(pomPath)) {
                Model model = reader.read(fileReader);

                System.out.println("Project: " + model.getArtifactId() + " " + model.getVersion());
                System.out.println("Checking dependencies in local repository: " + LOCAL_REPO_PATH);
                System.out.println("Checking for dependency upgrades...\n");

                // Check each dependency
                for (Dependency dependency : model.getDependencies()) {
                    checkDependency(dependency);
                }
            }

        } catch (Exception e) {
            System.out.println("Error reading pom.xml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets the local Maven repository path from settings.xml or uses default
     */
    private static String getLocalRepositoryPath() {
        // Try to get from M2_HOME or settings.xml
        String m2Home = System.getenv("M2_HOME");
        if (m2Home != null) {
            File settingsFile = new File(m2Home, "conf/settings.xml");
            if (settingsFile.exists()) {
                try {
                    String content = Files.readString(settingsFile.toPath());
                    int start = content.indexOf("<localRepository>");
                    if (start != -1) {
                        start += "<localRepository>".length();
                        int end = content.indexOf("</localRepository>", start);
                        if (end != -1) {
                            return content.substring(start, end);
                        }
                    }
                } catch (Exception e) {
                    // Fall back to default
                }
            }
        }

        // Default user home .m2 directory
        return System.getProperty("user.home") + "/.m2/repository";
    }

    /**
     * Checks for available versions of a dependency in both local and remote repositories
     */
    private static void checkDependency(Dependency dependency) {
        try {
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

            // Check local repository first
            List<String> localVersions = getLocalVersions(groupId, artifactId);

            System.out.println("  Current version: " + currentVersion);

            if (!localVersions.isEmpty()) {
                Collections.sort(localVersions);
                String latestLocalVersion = localVersions.get(localVersions.size() - 1);

                System.out.println("  Latest local version: " + latestLocalVersion);
                System.out.println("  Local versions: " + String.join(", ",
                        localVersions.size() <= 5 ? localVersions :
                                localVersions.subList(localVersions.size() - 5, localVersions.size())));

                // Show local repository path
                String localPath = getLocalRepositoryPath(groupId, artifactId, latestLocalVersion);
                System.out.println("  Local path: " + localPath);
            } else {
                System.out.println("  Not found in local repository");
            }

            // Then check remote repository
            try {
                List<MavenArtifact> remoteArtifacts = searchByCoordinates(groupId, artifactId);

                if (!remoteArtifacts.isEmpty()) {
                    MavenArtifact artifact = remoteArtifacts.get(0);
                    List<String> remoteVersions = new ArrayList<>(artifact.getAvailableVersions());
                    Collections.sort(remoteVersions);

                    String latestRemoteVersion = artifact.getLatestVersion();

                    System.out.println("  Latest remote version: " + latestRemoteVersion);

                    // Compare with current
                    if (currentVersion == null || latestRemoteVersion.compareTo(currentVersion) > 0) {
                        System.out.println("  UPGRADE AVAILABLE: " + latestRemoteVersion);
                    }

                    // Recent remote versions
                    int limit = Math.min(5, remoteVersions.size());
                    List<String> recentVersions = remoteVersions.subList(
                            Math.max(0, remoteVersions.size() - limit),
                            remoteVersions.size()
                    );

                    System.out.println("  Remote versions: " + String.join(", ", recentVersions));

                    // Show Maven Central URL
                    System.out.println("  Maven Central: https://search.maven.org/artifact/" +
                            groupId + "/" + artifactId);
                }
            } catch (Exception e) {
                System.out.println("  Error checking remote repository: " + e.getMessage());
            }

            System.out.println();

        } catch (Exception e) {
            System.out.println("  Error checking dependency: " + e.getMessage());
            System.out.println();
        }
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
     * Searches for Maven artifacts by group id and artifact id.
     */
    private static List<MavenArtifact> searchByCoordinates(String groupId, String artifactId)
            throws Exception {
        String query = "g:" + groupId + " AND a:" + artifactId;
        return searchArtifacts(query, 20);
    }

    /**
     * Searches for Maven artifacts by a simple query string.
     */
    private static List<MavenArtifact> searchArtifacts(String query, int rows) throws Exception {
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
     * Parses the JSON response from Maven Central.
     */
    private static List<MavenArtifact> parseSearchResults(String json) {
        List<MavenArtifact> results = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONObject response = jsonObject.getJSONObject("response");
            JSONArray docs = response.getJSONArray("docs");

            for (int i = 0; i < docs.length(); i++) {
                JSONObject doc = docs.getJSONObject(i);

                String groupId = doc.getString("g");
                String artifactId = doc.getString("a");
                String latestVersion = doc.optString("latestVersion", "");

                // Extract versions if available
                List<String> versions = new ArrayList<>();
                if (doc.has("v")) {
                    JSONArray vArray = doc.getJSONArray("v");
                    for (int j = 0; j < vArray.length(); j++) {
                        versions.add(vArray.getString(j));
                    }
                }

                results.add(new MavenArtifact(groupId, artifactId, latestVersion, versions));
            }
        } catch (Exception e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
        }

        return results;
    }

    /**
     * Class representing a Maven artifact.
     */
    private static class MavenArtifact {
        private final String groupId;
        private final String artifactId;
        private final String latestVersion;
        private final List<String> availableVersions;

        public MavenArtifact(String groupId, String artifactId, String latestVersion, List<String> availableVersions) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.latestVersion = latestVersion;
            this.availableVersions = availableVersions;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getLatestVersion() {
            return latestVersion;
        }

        public List<String> getAvailableVersions() {
            return availableVersions;
        }
    }
}