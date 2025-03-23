package com.joshuasalcedo.development;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Organization;
import org.apache.maven.model.v4.MavenStaxReader;

import java.io.FileReader;
public class PomLoader {
    public static void main (String[] args) {
        try {
            // Create a reader instance
            MavenStaxReader reader = new MavenStaxReader();

            // Load the POM file
            try (FileReader fileReader = new FileReader("pom.xml")) {
                Model model = reader.read(fileReader);
                Organization organization = model.getOrganization();


                // Now you can work with the Model object
                // After loading the model
                System.out.println("GroupId: " + model.getGroupId());
                System.out.println("ArtifactId: " + model.getArtifactId());
                System.out.println("Version: " + model.getVersion());
                // After loading the model
                System.out.println("Project: " + model.getArtifactId() + " " + model.getVersion());
                System.out.println("Dependencies Tree:");

// Get the list of dependencies
                var dependencies = model.getDependencies();
                for (var dependency : dependencies) {
                    System.out.println("├── " + dependency.getGroupId() + ":" +
                            dependency.getArtifactId() + ":" +
                            dependency.getVersion() +
                            (dependency.getScope() != null ? " (" + dependency.getScope() + ")" : ""));

                    // Note: To get transitive dependencies, you'd need a Maven development resolver
                    // as this information is not directly available in the POM model
                }


// Get and process dependencies

                System.out.println("Dependencies count: " + dependencies.size());

// Iterate through dependencies and print details
                dependencies.forEach(dependency -> {
                    System.out.println("-------------------");
                    System.out.println("GroupId: " + dependency.getGroupId());
                    System.out.println("ArtifactId: " + dependency.getArtifactId());
                    System.out.println("Version: " + dependency.getVersion());
                    System.out.println("Scope: " + dependency.getScope());
                    System.out.println("Type: " + dependency.getType());
                    System.out.println("Classifier: " + dependency.getClassifier());
                    System.out.println(dependency.getClass());

                    System.out.println("Exclusions count: " + dependency.getExclusions().size());
                    dependency.getExclusions().forEach(exclusion -> {
                        System.out.println("Exclusion GroupId: " + exclusion.getGroupId());
                    });
                    System.out.println("Classifier: " + dependency.getClassifier());
                    System.out.println("Optional: " + dependency.isOptional());
                    System.out.println("System Path: " + dependency.getSystemPath());
                    System.out.println("Management Key: " + dependency.getManagementKey());
                    System.out.println();
                    dependency.getLocationKeys().forEach(locationKey -> {
                        System.out.println("Location Key: " + locationKey);

                    });
                    System.out.println("-------------------");


                    // Maven Repository Link
                    var repositories = model.getRepositories();
                    System.out.println("Repositories count: " + repositories.size());

// Iterate through repositories and print details
                    repositories.forEach(repository -> {
                        System.out.println("-------------------");
                        System.out.println("Repository ID: " + repository.getId());
                        System.out.println("Repository URL: " + repository.getUrl());
                        System.out.println("Repository Name: " + repository.getName());

                        // Layout information
                        if (repository.getLayout() != null) {
                            System.out.println("Layout: " + repository.getLayout());
                        }

                        // Release policy
                        if (repository.getReleases() != null) {
                            System.out.println("Releases Enabled: " + repository.getReleases().isEnabled());
                            if (repository.getReleases().getUpdatePolicy() != null) {
                                System.out.println("Releases Update Policy: " + repository.getReleases().getUpdatePolicy());
                            }
                        }

                        // Snapshot policy
                        if (repository.getSnapshots() != null) {
                            System.out.println("Snapshots Enabled: " + repository.getSnapshots().isEnabled());
                            if (repository.getSnapshots().getUpdatePolicy() != null) {
                                System.out.println("Snapshots Update Policy: " + repository.getSnapshots().getUpdatePolicy());
                            }
                        }


                    });
                });
            }

        }  catch (Exception e) {
            e.printStackTrace();
        }

    }
}



