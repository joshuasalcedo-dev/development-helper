package com.joshuasalcedo.development.module;

import java.util.List;

/**
 * Interface to extend Module to support endpoints
 */
public interface EndpointAware {
    void addEndpoint(String endpoint);
    List<String> getEndpoints();
}