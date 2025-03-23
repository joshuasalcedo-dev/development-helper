package com.joshuasalcedo.development.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * Configuration properties for module tracking
 */
@Getter
@ConfigurationProperties(prefix = "module.tracking")
public class ModuleProperties {

    /**
     * Enable or disable module tracking
     */
    private boolean enabled = true;

    /**
     * Base path for module controller endpoints
     */
    private String basePath = "/application";

    /**
     * Whether to track method parameters
     */
    private boolean trackParameters = false;

    // Getters and setters

    public void setEnabled (boolean enabled) {
        this.enabled = enabled;
    }

    public void setBasePath (String basePath) {
        this.basePath = basePath;
    }

    public void setTrackParameters (boolean trackParameters) {
        this.trackParameters = trackParameters;
    }
}
