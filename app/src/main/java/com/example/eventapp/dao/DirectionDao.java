package com.example.eventapp.dao;

import com.example.eventapp.model.Direction;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DirectionDao {
    private final DataSource dataSource;

    public DirectionDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Direction> findAll() {
        String sql = "SELECT id, name FROM directions ORDER BY name";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<Direction> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to load directions", ex);
        }
    }

    public Direction getOrCreate(String name) {
        Optional<Direction> existing = findByName(name);
        if (existing.isPresent()) {
            return existing.get();
        }
        String sql = "INSERT INTO directions(name) VALUES(?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.executeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    return new Direction(rs.getLong(1), name);
                }
                throw new SQLException("Failed to read generated direction id");
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to create direction", ex);
        }
    }

    public Optional<Direction> findById(long id) {
        String sql = "SELECT id, name FROM directions WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to load direction", ex);
        }
    }

    public Optional<Direction> findByName(String name) {
        String sql = "SELECT id, name FROM directions WHERE LOWER(name) = LOWER(?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to load direction", ex);
        }
    }

    public List<String> findSimilarNames(String term, int limit) {
        String sql = "SELECT name FROM directions WHERE LOWER(name) LIKE LOWER(?) ORDER BY name LIMIT ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "%" + term + "%");
            statement.setInt(2, limit);
            try (ResultSet rs = statement.executeQuery()) {
                List<String> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(rs.getString("name"));
                }
                return result;
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to search directions", ex);
        }
    }

    private Direction mapRow(ResultSet rs) throws SQLException {
        return new Direction(rs.getLong("id"), rs.getString("name"));
    }
}
