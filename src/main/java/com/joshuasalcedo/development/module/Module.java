package com.joshuasalcedo.development.module;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Model class to store information about tracked modules
 */
public class Module implements EndpointAware {
    private final String name;
    private String className;
    private String methodName;
    private List<String> parameterTypes;
    private String errorMessage;
    private String successMessage;
    private AtomicLong callCounter;
    private List<String> calledBy;
    private List<String> endpoints;

    public Module(String name, String className, String methodName, List<String> parameterTypes,
                  String errorMessage, String successMessage) {
        this.name = name;
        this.className = className;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.errorMessage = errorMessage;
        this.successMessage = successMessage;
        this.callCounter = new AtomicLong(0);
        this.calledBy = new ArrayList<>();
        this.endpoints = new ArrayList<>();
    }

    public void incrementCounter() {
        callCounter.incrementAndGet();
    }

    public void addCaller(String caller) {
        if (!calledBy.contains(caller)) {
            calledBy.add(caller);
        }
    }

    @Override
    public void addEndpoint(String endpoint) {
        if (endpoint != null && !endpoints.contains(endpoint)) {
            endpoints.add(endpoint);
        }
    }

    @Override
    public List<String> getEndpoints() {
        return endpoints;
    }

    // Getters
    public String getName() { return name; }
    public String getClassName() { return className; }
    public String getMethodName() { return methodName; }
    public List<String> getParameterTypes() { return parameterTypes; }
    public String getErrorMessage() { return errorMessage; }
    public String getSuccessMessage() { return successMessage; }
    public long getCallCount() { return callCounter.get(); }
    public List<String> getCalledBy() { return calledBy; }
}