//package com.joshuasalcedo.development.model;
//
//
//
//import java.io.IOException;
//import java.net.URI;
//import java.net.URLEncoder;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//
///**
// * Utility class for searching Maven repositories for artifacts.
// */
//public class MavenRepositorySearcher {
//
//    private static final String MAVEN_CENTRAL_API = "https://search.maven.org/solrsearch/select";
//    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
//
//    /**
//     * Searches for Maven artifacts by a simple query string.
//     *
//     * @param query The search query (artifact id, group id, or keywords)
//     * @param rows Number of results to return (default 20)
//     * @return List of MavenArtifact objects
//     * @throws IOException If an I/O error occurs
//     * @throws InterruptedException If the operation is interrupted
//     */
//    public static List<MavenArtifact> searchArtifacts(String query, int rows) throws IOException, InterruptedException {
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
//     * Searches for Maven artifacts asynchronously.
//     *
//     * @param query The search query
//     * @param rows Number of results to return
//     * @return CompletableFuture of List of MavenArtifact objects
//     */
//    public static CompletableFuture<List<MavenArtifact>> searchArtifactsAsync(String query, int rows) {
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
//                .thenApply(MavenRepositorySearcher::parseSearchResults);
//    }
//
//    /**
//     * Searches for Maven artifacts by group id and artifact id.
//     *
//     * @param groupId The group ID
//     * @param artifactId The artifact ID
//     * @return List of MavenArtifact objects
//     * @throws IOException If an I/O error occurs
//     * @throws InterruptedException If the operation is interrupted
//     */
//    public static List<MavenArtifact> searchByCoordinates(String groupId, String artifactId)
//            throws IOException, InterruptedException {
//        String query = "g:" + groupId + " AND a:" + artifactId;
//        return searchArtifacts(query, 20);
//    }
//
//    /**
//     * Gets the latest version of a specific artifact.
//     *
//     * @param groupId The group ID
//     * @param artifactId The artifact ID
//     * @return The latest version or null if not found
//     * @throws IOException If an I/O error occurs
//     * @throws InterruptedException If the operation is interrupted
//     */
//    public static String getLatestVersion(String groupId, String artifactId)
//            throws IOException, InterruptedException {
//        List<MavenArtifact> artifacts = searchByCoordinates(groupId, artifactId);
//        return artifacts.isEmpty() ? null : artifacts.getFirst().getLatestVersion();
//    }
//
//    /**
//     * Parses the JSON response from Maven Central.
//     *
//     * @param json The JSON response
//     * @return List of MavenArtifact objects
//     */
//    private static List<MavenArtifact> parseSearchResults(String json) {
//        List<MavenArtifact> results = new ArrayList<>();
//        JSONObject jsonObject = new JSONObject(json);
//        JSONObject response = jsonObject.getJSONObject("response");
//        JSONArray docs = response.getJSONArray("docs");
//
//        for (int i = 0; i < docs.length(); i++) {
//            JSONObject doc = docs.getJSONObject(i);
//
//            String groupId = doc.getString("g");
//            String artifactId = doc.getString("a");
//            String latestVersion = doc.getString("latestVersion");
//
//            // Extract versions if available
//            List<String> versions = new ArrayList<>();
//            if (doc.has("v")) {
//                JSONArray vArray = doc.getJSONArray("v");
//                for (int j = 0; j < vArray.length(); j++) {
//                    versions.add(vArray.getString(j));
//                }
//            }
//
//            results.add(new MavenArtifact(groupId, artifactId, latestVersion, versions));
//        }
//
//        return results;
//    }
//
//    /**
//     * Class representing a Maven artifact.
//     */
//    public static class MavenArtifact {
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
//
//        public String getCoordinates() {
//            return groupId + ":" + artifactId + ":" + latestVersion;
//        }
//
//        @Override
//        public String toString() {
//            return "MavenArtifact{" +
//                    "groupId='" + groupId + '\'' +
//                    ", artifactId='" + artifactId + '\'' +
//                    ", latestVersion='" + latestVersion + '\'' +
//                    ", availableVersions=" + availableVersions +
//                    '}';
//        }
//    }
//}