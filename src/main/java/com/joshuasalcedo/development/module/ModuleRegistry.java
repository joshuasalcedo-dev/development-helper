package com.joshuasalcedo.development.module;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for tracking module usage
 */
@Component
public class ModuleRegistry {
    private final Map<String, Module> modules = new ConcurrentHashMap<>();
    private final Map<String, String> methodToModuleMap = new ConcurrentHashMap<>();

    public void registerModule(String key, Module module) {
        modules.put(key, module);
        methodToModuleMap.put(module.getClassName() + "." + module.getMethodName(), key);
    }

    public Module getModule(String key) {
        return modules.get(key);
    }

    public List<Module> getAllModules() {
        return new ArrayList<>(modules.values());
    }

    public void trackMethodCall(String className, String methodName, String caller) {
        String methodKey = className + "." + methodName;
        String moduleKey = methodToModuleMap.get(methodKey);

        if (moduleKey != null) {
            Module module = modules.get(moduleKey);
            if (module != null) {
                module.incrementCounter();
                if (caller != null) {
                    module.addCaller(caller);
                }
            }
        }
    }
}