package com.example.eventapp.db;

import com.example.eventapp.config.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * Configures a HikariCP connection pool for the application.
 */
public final class Database implements AutoCloseable {
    private final HikariDataSource dataSource;

    public Database(DatabaseConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getJdbcUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setMaximumPoolSize(config.getMaximumPoolSize());
        hikariConfig.setDriverClassName(config.getDriverClassName());
        hikariConfig.setPoolName("event-app-pool");
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setAutoCommit(true);
        this.dataSource = new HikariDataSource(hikariConfig);
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
