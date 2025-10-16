package com.example.eventapp.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DatabaseConfig {
    private final Properties properties = new Properties();

    public DatabaseConfig() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (in == null) {
                throw new IllegalStateException("application.properties not found in classpath");
            }
            properties.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }
    }

    public String getJdbcUrl() {
        return requireProperty("app.datasource.jdbcUrl");
    }

    public String getUsername() {
        return requireProperty("app.datasource.username");
    }

    public String getPassword() {
        return requireProperty("app.datasource.password");
    }

    public int getMaximumPoolSize() {
        return Integer.parseInt(properties.getProperty("app.datasource.maximumPoolSize", "10"));
    }

    private String requireProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Property '%s' is missing or empty".formatted(key));
        }
        return value;
    }
}
