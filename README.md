# Maven Dependency Management Library

A comprehensive Java library for analyzing and managing Maven dependencies across multiple repositories.

## Features

- **Dependency Analysis**: Parse Maven POM files and analyze dependencies
- **Version Checking**: Identify outdated dependencies and available updates
- **Conflict Detection**: Find and resolve dependency conflicts
- **Security Scanning**: Check dependencies for known security vulnerabilities
- **Multi-Repository Support**: Search for dependencies across multiple Maven repositories
- **Local Repository Integration**: Check dependencies against your local Maven repository
- **Module System**: Modular architecture for extending functionality

## Getting Started

### Prerequisites

- Java 23 or higher
- Maven 4.0.0 or higher

### Installation

Add the dependency to your project's `pom.xml`:

```xml
<dependency>
    <groupId>com.joshuasalcedo</groupId>
    <artifactId>development-helper</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

### Basic Dependency Analysis

```java
import com.joshuasalcedo.development.dependency.DependencyManager;
import com.joshuasalcedo.development.dependency.Project;
import java.io.File;

public class Example {
    public static void main(String[] args) throws Exception {
        DependencyManager manager = new DependencyManager();
        
        // Parse a POM file
        File pomFile = new File("path/to/pom.xml");
        Project project = manager.parseProject(pomFile);
        
        // Analyze project with security checks
        Project analyzed = manager.analyzeProject(pomFile, true);
        
        // Get outdated dependencies
        List<Dependency> outdated = manager.getOutdatedDependencies(analyzed);
        outdated.forEach(dep -> System.out.println(
            dep.getGroupId() + ":" + dep.getArtifactId() + " - Current: " + 
            dep.getVersion() + ", Latest: " + dep.getLatestVersion()
        ));
    }
}
```

### Multi-Repository Dependency Checking

Use the `MultiRepoDependencyChecker` to check dependencies across multiple Maven repositories:

```java
import com.joshuasalcedo.development.MultiRepoDependencyChecker;

public class Example {
    public static void main(String[] args) {
        // This will check all dependencies in your pom.xml 
        // across multiple repositories (Maven Central, Google, JCenter, etc.)
        MultiRepoDependencyChecker.main(new String[]{});
    }
}
```

## Module System

The library uses a modular architecture, allowing you to extend functionality:

```java
import com.joshuasalcedo.development.module.Module;
import com.joshuasalcedo.development.module.ApplicationModule;

// Create a custom module
public class MyCustomModule implements Module {
    @Override
    public void initialize() {
        // Module initialization
    }
    
    @Override
    public String getName() {
        return "MyCustomModule";
    }
}

// Register in your application
ApplicationModule.registerModule(new MyCustomModule());
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.