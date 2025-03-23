//package com.joshuasalcedo.development.model;
//
//
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//
//public class MavenSearchExample {
//    public static void main(String[] args) {
//        try {
//            // Search for Spring Boot starters
//            System.out.println("Searching for Spring Boot starters...");
//            List<MavenRepositorySearcher.MavenArtifact> springArtifacts =
//                    MavenRepositorySearcher.searchArtifacts("spring-boot-starter", 5);
//
//            springArtifacts.forEach(artifact -> {
//                System.out.println("Found: " + artifact.getCoordinates());
//            });
//
//            // Get the latest version of a specific artifact
//            String latestJunit = MavenRepositorySearcher.getLatestVersion("junit", "junit");
//            System.out.println("\nLatest JUnit version: " + latestJunit);
//
//            // Asynchronous search example
//            System.out.println("\nAsynchronously searching for Jackson...");
//            CompletableFuture<List<MavenRepositorySearcher.MavenArtifact>> futureResults =
//                    MavenRepositorySearcher.searchArtifactsAsync("com.fasterxml.jackson.core", 3);
//
//            futureResults.thenAccept(results -> {
//                results.forEach(artifact -> {
//                    System.out.println("Found async: " + artifact.getCoordinates());
//                });
//            }).join(); // Wait for completion in this example
//
//            // Search by specific coordinates
//            System.out.println("\nSearching for specific artifact...");
//            List<MavenRepositorySearcher.MavenArtifact> lombokVersions =
//                    MavenRepositorySearcher.searchByCoordinates("org.projectlombok", "lombok");
//
//            if (!lombokVersions.isEmpty()) {
//                MavenRepositorySearcher.MavenArtifact lombok = lombokVersions.get(0);
//                System.out.println("Lombok versions available: " + lombok.getAvailableVersions().size());
//                System.out.println("Latest version: " + lombok.getLatestVersion());
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}