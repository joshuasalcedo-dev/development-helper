package com.joshuasalcedo.development.module;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller to expose module information via REST API
 */
@RestController
public class ModuleController {

    private final ModuleRegistry registry;
    private final String basePath;

    public ModuleController (ModuleRegistry registry, String basePath) {
        this.registry = registry;
        this.basePath = basePath;
    }

    /**
     * Get all application modules
     */
    @GetMapping("${basePath}/modules")
    public ResponseEntity<List<Module>> getAllModules () {
        return ResponseEntity.ok(registry.getAllModules());
    }

    /**
     * Get application modules for a specific app
     */
    @GetMapping("${basePath}/{appName}/modules")
    public ResponseEntity<List<Module>> getAppModules (@PathVariable String appName) {
        List<Module> appModules = registry.getAllModules().stream()
                .filter(module -> module.getName().startsWith(appName + "."))
                .collect(Collectors.toList());

        return ResponseEntity.ok(appModules);
    }
}