package com.soprasteria.devopsacademy;

import org.glassfish.jersey.server.ResourceConfig;

import java.util.Map;

public class ApplicationApiConfig extends ResourceConfig {

    public ApplicationApiConfig() {
        super(TodoApi.class);
        setProperties(Map.of("jersey.config.server.wadl.disableWadl", "true"));
    }
}
