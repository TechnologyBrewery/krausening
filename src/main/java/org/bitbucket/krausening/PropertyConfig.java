package org.bitbucket.krausening;

import java.util.List;

import com.typesafe.config.Config;

public class PropertyConfig {
    private String name;
    private List<String> properties;

    public PropertyConfig(final Config params) {
        this.name = params.getString("name");
        this.properties = params.getStringList("properties");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getProperties() {
        return properties;
    }

    public void setProperties(List<String> properties) {
        this.properties = properties;
    }
}