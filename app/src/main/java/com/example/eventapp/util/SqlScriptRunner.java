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

public final class SqlScriptRunner {
    private SqlScriptRunner() {
    }

    public static void runScript(DataSource dataSource, String resourcePath) throws IOException, SQLException {
        try (InputStream in = SqlScriptRunner.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("SQL script not found: " + resourcePath);
            }
            String sql = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
            executeSql(dataSource, sql);
        }
    }

    private static void executeSql(DataSource dataSource, String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (String command : sql.split(";\\s*\n")) {
                String trimmed = command.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
        }
    }
}
