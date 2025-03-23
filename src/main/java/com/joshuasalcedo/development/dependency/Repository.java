package com.joshuasalcedo.development.dependency;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * POJO representing a Maven repository
 */
@Getter
@Setter
@Builder
public class Repository {
    private String id;
    private String name;
    private String url;
    private String type;
    private boolean isLocal;

    // Default constructor
    public Repository() {
    }

    // Constructor with essential fields
    public Repository(String id, String url) {
        this.id = id;
        this.url = url;
    }

    // Constructor with all fields
    public Repository(String id, String name, String url, String type, boolean isLocal) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.type = type;
        this.isLocal = isLocal;
    }




    @Override
    public String toString() {
        return name != null ? name + " (" + url + ")" : (id != null ? id + " (" + url + ")" : url);
    }
}