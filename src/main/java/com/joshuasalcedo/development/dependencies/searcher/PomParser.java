package com.joshuasalcedo.development.dependencies.searcher;



import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

/**
 * Utility for retrieving and parsing POM files to extract repository URLs
 */
public class PomParser {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    /**
     * Fetches and parses a POM file to extract repository information
     *
     * @param groupId The group ID
     * @param artifactId The artifact ID
     * @param version The version
     * @return Optional containing repository information if found
     */
    public static Optional<RepositoryInfo> getRepositoryInfo(String groupId, String artifactId, String version) {
        try {
            // Construct the POM URL
            String pomUrl = String.format(
                    "https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.pom",
                    groupId.replace('.', '/'),
                    artifactId,
                    version,
                    artifactId,
                    version
            );

            // Fetch the POM
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(pomUrl))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

            // Parse the POM
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new InputStreamReader(response.body()));

            // Extract repository information
            RepositoryInfo info = new RepositoryInfo();

            if (model.getScm() != null) {
                info.setScmUrl(model.getScm().getUrl());
                info.setScmConnection(model.getScm().getConnection());
                info.setScmDeveloperConnection(model.getScm().getDeveloperConnection());
                info.setScmTag(model.getScm().getTag());
            }

            if (model.getUrl() != null) {
                info.setProjectUrl(model.getUrl());
            }

            if (model.getIssueManagement() != null) {
                info.setIssueTrackerUrl(model.getIssueManagement().getUrl());
                info.setIssueTrackerSystem(model.getIssueManagement().getSystem());
            }

            if (model.getCiManagement() != null) {
                info.setCiSystem(model.getCiManagement().getSystem());
                info.setCiUrl(model.getCiManagement().getUrl());
            }

            if (model.getOrganization() != null) {
                info.setOrganizationName(model.getOrganization().getName());
                info.setOrganizationUrl(model.getOrganization().getUrl());
            }

            // Extract GitHub URL if possible
            String githubUrl = extractGithubUrl(info);
            if (githubUrl != null) {
                info.setGithubUrl(githubUrl);
            }

            return Optional.of(info);

        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Attempts to extract a GitHub URL from the repository information
     */
    private static String extractGithubUrl(RepositoryInfo info) {
        // Check all possible sources for a GitHub URL
        if (info.getScmUrl() != null && info.getScmUrl().contains("github.com")) {
            return cleanGithubUrl(info.getScmUrl());
        }

        if (info.getScmConnection() != null && info.getScmConnection().contains("github.com")) {
            return extractGithubFromScmConnection(info.getScmConnection());
        }

        if (info.getScmDeveloperConnection() != null && info.getScmDeveloperConnection().contains("github.com")) {
            return extractGithubFromScmConnection(info.getScmDeveloperConnection());
        }

        if (info.getProjectUrl() != null && info.getProjectUrl().contains("github.com")) {
            return cleanGithubUrl(info.getProjectUrl());
        }

        if (info.getIssueTrackerUrl() != null && info.getIssueTrackerUrl().contains("github.com")) {
            String url = info.getIssueTrackerUrl();
            // Convert GitHub issues URL to repo URL
            return url.replaceAll("/issues.*$", "");
        }

        return null;
    }

    /**
     * Cleans a GitHub URL to ensure it's in the standard format
     */
    private static String cleanGithubUrl(String url) {
        // Remove any .git suffix
        String cleaned = url.replaceAll("\\.git$", "");

        // Ensure it starts with https://
        if (cleaned.startsWith("git@github.com:")) {
            cleaned = "https://github.com/" + cleaned.substring("git@github.com:".length());
        } else if (cleaned.startsWith("scm:git:git@github.com:")) {
            cleaned = "https://github.com/" + cleaned.substring("scm:git:git@github.com:".length());
        }

        // Remove any trailing slashes
        return cleaned.replaceAll("/$", "");
    }

    /**
     * Extracts a GitHub URL from an SCM connection string
     */
    private static String extractGithubFromScmConnection(String connection) {
        // Handle formats like: scm:git:git@github.com:org/repo.git
        if (connection.contains("github.com")) {
            int idx = connection.indexOf("github.com");

            // Find the start of org/repo part
            int start = connection.indexOf(':', idx);
            if (start == -1) {
                start = connection.indexOf('/', idx);
            }

            if (start != -1) {
                start++; // Skip the : or /

                // Find the end
                int end = connection.indexOf(".git", start);
                if (end == -1) {
                    end = connection.length();
                }

                String orgRepo = connection.substring(start, end);
                return "https://github.com/" + orgRepo;
            }
        }

        return null;
    }

    /**
     * Class to hold repository information extracted from a POM
     */
    public static class RepositoryInfo {
        private String scmUrl;
        private String scmConnection;
        private String scmDeveloperConnection;
        private String scmTag;
        private String projectUrl;
        private String issueTrackerUrl;
        private String issueTrackerSystem;
        private String ciSystem;
        private String ciUrl;
        private String organizationName;
        private String organizationUrl;
        private String githubUrl;

        // Getters and setters

        public String getScmUrl() {
            return scmUrl;
        }

        public void setScmUrl(String scmUrl) {
            this.scmUrl = scmUrl;
        }

        public String getScmConnection() {
            return scmConnection;
        }

        public void setScmConnection(String scmConnection) {
            this.scmConnection = scmConnection;
        }

        public String getScmDeveloperConnection() {
            return scmDeveloperConnection;
        }

        public void setScmDeveloperConnection(String scmDeveloperConnection) {
            this.scmDeveloperConnection = scmDeveloperConnection;
        }

        public String getScmTag() {
            return scmTag;
        }

        public void setScmTag(String scmTag) {
            this.scmTag = scmTag;
        }

        public String getProjectUrl() {
            return projectUrl;
        }

        public void setProjectUrl(String projectUrl) {
            this.projectUrl = projectUrl;
        }

        public String getIssueTrackerUrl() {
            return issueTrackerUrl;
        }

        public void setIssueTrackerUrl(String issueTrackerUrl) {
            this.issueTrackerUrl = issueTrackerUrl;
        }

        public String getIssueTrackerSystem() {
            return issueTrackerSystem;
        }

        public void setIssueTrackerSystem(String issueTrackerSystem) {
            this.issueTrackerSystem = issueTrackerSystem;
        }

        public String getCiSystem() {
            return ciSystem;
        }

        public void setCiSystem(String ciSystem) {
            this.ciSystem = ciSystem;
        }

        public String getCiUrl() {
            return ciUrl;
        }

        public void setCiUrl(String ciUrl) {
            this.ciUrl = ciUrl;
        }

        public String getOrganizationName() {
            return organizationName;
        }

        public void setOrganizationName(String organizationName) {
            this.organizationName = organizationName;
        }

        public String getOrganizationUrl() {
            return organizationUrl;
        }

        public void setOrganizationUrl(String organizationUrl) {
            this.organizationUrl = organizationUrl;
        }

        public String getGithubUrl() {
            return githubUrl;
        }

        public void setGithubUrl(String githubUrl) {
            this.githubUrl = githubUrl;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Repository Information:\n");

            if (githubUrl != null) {
                sb.append("GitHub URL: ").append(githubUrl).append("\n");
            }

            if (scmUrl != null) {
                sb.append("SCM URL: ").append(scmUrl).append("\n");
            }

            if (projectUrl != null) {
                sb.append("Project URL: ").append(projectUrl).append("\n");
            }

            if (issueTrackerUrl != null) {
                sb.append("Issue Tracker: ");
                if (issueTrackerSystem != null) {
                    sb.append(issueTrackerSystem).append(" - ");
                }
                sb.append(issueTrackerUrl).append("\n");
            }

            if (ciSystem != null || ciUrl != null) {
                sb.append("CI: ");
                if (ciSystem != null) {
                    sb.append(ciSystem).append(" - ");
                }
                if (ciUrl != null) {
                    sb.append(ciUrl);
                }
                sb.append("\n");
            }

            if (organizationName != null || organizationUrl != null) {
                sb.append("Organization: ");
                if (organizationName != null) {
                    sb.append(organizationName).append(" - ");
                }
                if (organizationUrl != null) {
                    sb.append(organizationUrl);
                }
                sb.append("\n");
            }

            return sb.toString();
        }
    }
}
