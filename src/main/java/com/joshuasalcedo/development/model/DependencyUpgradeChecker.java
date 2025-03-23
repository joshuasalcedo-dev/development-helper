//package com.joshuasalcedo.development.model;
//
//import org.apache.maven.api.model.Dependency;
//import org.apache.maven.api.model.Model;
//import org.apache.maven.model.v4.MavenStaxReader;
//
//import java.io.FileReader;
//import java.net.URI;
//import java.net.URLEncoder;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//import java.util.stream.Collectors;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//
///**
// * Utility to check for available upgrades of current dependencies
// * using the modern Maven API model and MavenStaxReader
// */
//public class DependencyUpgradeChecker {
//    private static final String MAVEN_CENTRAL_API = "https://search.maven.org/solrsearch/select";
//    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
//
//    public static void main(String[] args) {
//        try {
//            // Path to your project's pom.xml file
//            String pomPath = "pom.xml";
//
//            // Create a reader instance
//            MavenStaxReader reader = new MavenStaxReader();
//
//            // Load the POM file
//            try (FileReader fileReader = new FileReader(pomPath)) {
//                Model model = reader.read(fileReader);
//
//                System.out.println("Project: " + model.getArtifactId() + " " + model.getVersion());
//                System.out.println("Checking for dependency upgrades...\n");
//
//                // Check each dependency
//                for (Dependency dependency : model.getDependencies()) {
//                    checkForUpgrades(dependency);
//                }
//            }
//
//        } catch (Exception e) {
//            System.out.println("Error reading pom.xml: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Checks for available upgrades for a specific dependency
//     */
//    private static void checkForUpgrades(Dependency dependency) {
//        try {
//            String groupId = dependency.getGroupId();
//            String artifactId = dependency.getArtifactId();
//            String currentVersion = dependency.getVersion();
//
//            // Handle property placeholders in version
//            if (currentVersion != null && currentVersion.startsWith("${") && currentVersion.endsWith("}")) {
//                System.out.println("Skipping " + groupId + ":" + artifactId +
//                        " - Version uses property placeholder: " + currentVersion);
//                return;
//            }
//
//            System.out.println("Checking " + groupId + ":" + artifactId + ":" + currentVersion);
//
//            // Get available versions
//            List<MavenArtifact> artifacts = searchByCoordinates(groupId, artifactId);
//
//            if (!artifacts.isEmpty()) {
//                MavenArtifact artifact = artifacts.get(0);
//                List<String> availableVersions = new ArrayList<>(artifact.getAvailableVersions());
//
//                // Sort versions (simple string comparison, not semantic versioning)
//                Collections.sort(availableVersions);
//
//                // Get the latest version
//                String latestVersion = artifact.getLatestVersion();
//
//                System.out.println("  Current version: " + currentVersion);
//                System.out.println("  Latest version: " + latestVersion);
//
//                // Compare versions (simple string comparison)
//                if (currentVersion == null || latestVersion.compareTo(currentVersion) > 0) {
//                    System.out.println("  UPGRADE AVAILABLE: " + latestVersion);
//
//                    // Show Maven Central URL
//                    System.out.println("  Maven Central: https://search.maven.org/artifact/" +
//                            groupId + "/" + artifactId);
//
//                    // Show GitHub URL if possible
//                    String githubUrl = inferGithubUrl(groupId, artifactId);
//                    if (githubUrl != null) {
//                        System.out.println("  GitHub (inferred): " + githubUrl);
//                    }
//                } else {
//                    System.out.println("  Up to date");
//                }
//
//                // Show recent versions (last 5)
//                int limit = Math.min(5, availableVersions.size());
//                List<String> recentVersions = availableVersions.subList(
//                        Math.max(0, availableVersions.size() - limit),
//                        availableVersions.size()
//                );
//
//                System.out.println("  Recent versions: " +
//                        recentVersions.stream().collect(Collectors.joining(", ")));
//
//                System.out.println();
//            } else {
//                System.out.println("  No information found for this dependency");
//                System.out.println();
//            }
//
//        } catch (Exception e) {
//            System.out.println("  Error checking for updates: " + e.getMessage());
//            System.out.println();
//        }
//    }
//
//    /**
//     * Searches for Maven artifacts by group id and artifact id.
//     */
//    private static List<MavenArtifact> searchByCoordinates(String groupId, String artifactId)
//            throws Exception {
//        String query = "g:" + groupId + " AND a:" + artifactId;
//        return searchArtifacts(query, 20);
//    }
//
//    /**
//     * Searches for Maven artifacts by a simple query string.
//     */
//    private static List<MavenArtifact> searchArtifacts(String query, int rows) throws Exception {
//        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
//        String url = MAVEN_CENTRAL_API + "?q=" + encodedQuery + "&rows=" + rows + "&wt=json";
//
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(url))
//                .header("Accept", "application/json")
//                .GET()
//                .build();
//
//        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
//        return parseSearchResults(response.body());
//    }
//
//    /**
//     * Asynchronous version of the search method.
//     */
//    private static CompletableFuture<List<MavenArtifact>> searchArtifactsAsync(String query, int rows) {
//        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
//        String url = MAVEN_CENTRAL_API + "?q=" + encodedQuery + "&rows=" + rows + "&wt=json";
//
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(url))
//                .header("Accept", "application/json")
//                .GET()
//                .build();
//
//        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
//                .thenApply(HttpResponse::body)
//                .thenApply(DependencyUpgradeChecker::parseSearchResults);
//    }
//
//    /**
//     * Parses the JSON response from Maven Central.
//     */
//    private static List<MavenArtifact> parseSearchResults(String json) {
//        List<MavenArtifact> results = new ArrayList<>();
//        try {
//            JSONObject jsonObject = new JSONObject(json);
//            JSONObject response = jsonObject.getJSONObject("response");
//            JSONArray docs = response.getJSONArray("docs");
//
//            for (int i = 0; i < docs.length(); i++) {
//                JSONObject doc = docs.getJSONObject(i);
//
//                String groupId = doc.getString("g");
//                String artifactId = doc.getString("a");
//                String latestVersion = doc.optString("latestVersion", "");
//
//                // Extract versions if available
//                List<String> versions = new ArrayList<>();
//                if (doc.has("v")) {
//                    JSONArray vArray = doc.getJSONArray("v");
//                    for (int j = 0; j < vArray.length(); j++) {
//                        versions.add(vArray.getString(j));
//                    }
//                }
//
//                results.add(new MavenArtifact(groupId, artifactId, latestVersion, versions));
//            }
//        } catch (Exception e) {
//            System.err.println("Error parsing JSON: " + e.getMessage());
//        }
//
//        return results;
//    }
//
//    /**
//     * Attempts to determine the GitHub URL based on groupId and artifactId.
//     */
//    private static String inferGithubUrl(String groupId, String artifactId) {
//        // Try to derive GitHub URL from groupId
//        String[] groupParts = groupId.split("\\.");
//        if (groupParts.length >= 2) {
//            String organization = groupParts[1];
//            if (groupParts[0].equals("com") || groupParts[0].equals("org") || groupParts[0].equals("io")) {
//                if (groupParts[1].equals("github")) {
//                    // Handle com.github.user format
//                    if (groupParts.length >= 3) {
//                        return "https://github.com/" + groupParts[2] + "/" + artifactId;
//                    }
//                } else {
//                    // Handle common formats like com.organization.project
//                    return "https://github.com/" + organization + "/" + artifactId;
//                }
//            }
//        }
//
//        // If all else fails, return a search URL
//        return "https://github.com/search?q=" + groupId + "+" + artifactId;
//    }
//
//    /**
//     * Class representing a Maven artifact.
//     */
//    private static class MavenArtifact {
//        private final String groupId;
//        private final String artifactId;
//        private final String latestVersion;
//        private final List<String> availableVersions;
//
//        public MavenArtifact(String groupId, String artifactId, String latestVersion, List<String> availableVersions) {
//            this.groupId = groupId;
//            this.artifactId = artifactId;
//            this.latestVersion = latestVersion;
//            this.availableVersions = availableVersions;
//        }
//
//        public String getGroupId() {
//            return groupId;
//        }
//
//        public String getArtifactId() {
//            return artifactId;
//        }
//
//        public String getLatestVersion() {
//            return latestVersion;
//        }
//
//        public List<String> getAvailableVersions() {
//            return availableVersions;
//        }
//    }
//}