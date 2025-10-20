package com.example.eventapp.util;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Utility that executes SQL scripts shipped inside the application's resources.
 */
public final class SqlScriptRunner {

    private SqlScriptRunner() {
    }

    public static void runScript(DataSource dataSource, String resourcePath) {
        try (InputStream inputStream = SqlScriptRunner.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Resource '" + resourcePath + "' not found");
            }
            String sql = new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .collect(Collectors.joining("\n"));
            executeSql(dataSource, sql);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load SQL resource: " + resourcePath, e);
        }
    }

    private static void executeSql(DataSource dataSource, String sql) {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (String part : sql.split(";")) {
                String trimmed = part.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                statement.execute(trimmed);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to execute SQL script", e);
        }
    }
}
