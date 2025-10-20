package com.example.eventapp.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/**
 * Loads JDBC/Hikari configuration from {@code application.properties} file located on the classpath.
 */
public final class DatabaseConfig {
    private final Properties properties = new Properties();

    public DatabaseConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IllegalStateException("application.properties not found on classpath");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load database configuration", e);
        }
    }

    public String getJdbcUrl() {
        return require("app.datasource.jdbcUrl");
    }

    public String getUsername() {
        return require("app.datasource.username");
    }

    public String getPassword() {
        return require("app.datasource.password");
    }

    public String getDriverClassName() {
        return properties.getProperty("app.datasource.driverClassName", "com.mysql.cj.jdbc.Driver");
    }

    public int getMaximumPoolSize() {
        String value = properties.getProperty("app.datasource.maximumPoolSize", "10");
        return Integer.parseInt(value);
    }

    private String require(String key) {
        String value = properties.getProperty(key);
        if (Objects.requireNonNullElse(value, "").isBlank()) {
            throw new IllegalStateException("Property '" + key + "' must be provided in application.properties");
        }
        return value;
    }
}
