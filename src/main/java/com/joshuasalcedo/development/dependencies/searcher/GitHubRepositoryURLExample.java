package com.joshuasalcedo.development.dependencies.searcher;



/**
 * Example demonstrating how to get GitHub repository URLs for Maven artifacts
 */
public class GitHubRepositoryURLExample {
    public static void main(String[] args) {
        try {
            // Libraries to check
            String[][] libraries = {
                    {"org.springframework.boot", "spring-boot-starter"},
                    {"com.google.guava", "guava"},
                    {"org.apache.commons", "commons-lang3"},
                    {"lombok", "lombok"},
                    {"com.fasterxml.jackson.core", "jackson-databind"}
            };

            System.out.println("Fetching GitHub repository URLs for popular libraries...\n");

            for (String[] library : libraries) {
                String groupId = library[0];
                String artifactId = library[1];

                // Get the latest version
                String version = MavenRepositorySearcher.getLatestVersion(groupId, artifactId);

                if (version != null) {
                    System.out.println("Library: " + groupId + ":" + artifactId + ":" + version);

                    // Get detailed repository information from POM
                    PomParser.getRepositoryInfo(groupId, artifactId, version)
                            .ifPresent(repoInfo -> {
                                System.out.println(repoInfo.toString());

                                // Highlight the best URL to use (prioritizing GitHub)
                                String bestUrl = repoInfo.getGithubUrl() != null
                                        ? repoInfo.getGithubUrl()
                                        : (repoInfo.getScmUrl() != null
                                        ? repoInfo.getScmUrl()
                                        : repoInfo.getProjectUrl());

                                if (bestUrl != null) {
                                    System.out.println("BEST REPOSITORY URL: " + bestUrl);
                                }
                            });

                    System.out.println("----------------------------------------------\n");
                }
            }

            // Demonstrate directly getting repository info for a specific known artifact
            System.out.println("Direct repository lookup for JUnit Jupiter:");
            PomParser.getRepositoryInfo("org.junit.jupiter", "junit-jupiter", "5.9.2")
                    .ifPresent(System.out::println);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}