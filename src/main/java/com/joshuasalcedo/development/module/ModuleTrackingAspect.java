package com.joshuasalcedo.development.module;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Aspect to intercept method calls and track usage
 */
@Component
public class ModuleTrackingAspect {
    @Autowired
    private ModuleRegistry registry;

    // Using AspectJ would be more elegant, but here's a simplified approach
    // You would need to add @Around advice to intercept method calls

    public void trackMethodCall(String className, String methodName, String caller) {
        registry.trackMethodCall(className, methodName, caller);
    }
}