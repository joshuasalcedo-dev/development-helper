package com.joshuasalcedo.development.dependency;

/**
 * POJO representing a Maven repository
 */
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

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public void setLocal(boolean local) {
        isLocal = local;
    }

    @Override
    public String toString() {
        return name != null ? name + " (" + url + ")" : (id != null ? id + " (" + url + ")" : url);
    }
}