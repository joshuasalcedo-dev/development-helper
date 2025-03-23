package com.joshuasalcedo.development.dependencies.searcher;



import java.util.List;

public class RepositoryUrlExample {
    public static void main(String[] args) {
        try {
            // Popular libraries that likely have repository information
            System.out.println("Finding repository URLs for popular libraries...\n");

            String[][] librariesToCheck = {
                    {"org.springframework.boot", "spring-boot-starter"},
                    {"com.google.guava", "guava"},
                    {"org.apache.commons", "commons-lang3"},
                    {"com.fasterxml.jackson.core", "jackson-databind"},
                    {"org.projectlombok", "lombok"}
            };

            for (String[] library : librariesToCheck) {
                List<MavenRepositorySearcher.MavenArtifact> results =
                        MavenRepositorySearcher.searchByCoordinates(library[0], library[1]);

                if (!results.isEmpty()) {
                    MavenRepositorySearcher.MavenArtifact artifact = results.get(0);

                    System.out.println("Library: " + artifact.getCoordinates());

                    // Direct SCM URL from Maven metadata if available
                    String scmUrl = artifact.getRepositoryUrl();
                    System.out.println("SCM URL: " + (scmUrl != null ? scmUrl : "Not available in metadata"));

                    // Inferred GitHub URL based on naming conventions
                    System.out.println("GitHub URL (best guess): " + artifact.getGithubUrl());

                    // Maven Central URL (always available)
                    System.out.println("Maven Central URL: " + artifact.getMavenCentralUrl());

                    System.out.println();
                }
            }

            // Extended info for a specific artifact
            System.out.println("\n--- Detailed Repository Information ---");
            System.out.println("Getting extended repository info for Lombok...");

            // Call to get POM URL and extract more detailed information
            String groupId = "org.projectlombok";
            String artifactId = "lombok";
            String version = MavenRepositorySearcher.getLatestVersion(groupId, artifactId);

            String pomUrl = String.format(
                    "https://search.maven.org/remotecontent?filepath=%s/%s/%s/%s-%s.pom",
                    groupId.replace('.', '/'),
                    artifactId,
                    version,
                    artifactId,
                    version
            );

            System.out.println("POM URL: " + pomUrl);
            System.out.println("You can fetch and parse this POM to get detailed SCM/repository information");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}