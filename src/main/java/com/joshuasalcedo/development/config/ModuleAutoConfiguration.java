package com.joshuasalcedo.development.config;

import com.joshuasalcedo.development.module.ModuleController;
import com.joshuasalcedo.development.module.ModuleRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(ModuleRegistry.class)
@EnableConfigurationProperties(ModuleProperties.class)
public class ModuleAutoConfiguration {

    private final ModuleProperties properties;

    public ModuleAutoConfiguration (ModuleProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnProperty(prefix = "module.tracking", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ModuleRegistry moduleRegistry () {
        return new ModuleRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "module.tracking", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ModuleController moduleController (ModuleRegistry moduleRegistry, ModuleProperties properties) {
        return new ModuleController(moduleRegistry, properties.getBasePath());
    }
}