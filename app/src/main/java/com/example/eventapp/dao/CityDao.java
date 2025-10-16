package com.example.eventapp.dao;

import com.example.eventapp.model.City;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CityDao {
    private final DataSource dataSource;

    public CityDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<City> findAll() {
        String sql = "SELECT id, name_ru, country_code FROM cities ORDER BY name_ru";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<City> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to load cities", ex);
        }
    }

    public Optional<City> findById(int id) {
        String sql = "SELECT id, name_ru, country_code FROM cities WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to find city", ex);
        }
    }

    public Optional<City> findByName(String name) {
        String sql = "SELECT id, name_ru, country_code FROM cities WHERE LOWER(name_ru) = LOWER(?)";
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
            throw new RuntimeException("Failed to find city", ex);
        }
    }

    public City insertCity(String name, String countryCode) {
        String sql = "INSERT INTO cities(id, name_ru, country_code) VALUES(?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int newId = generateNextId(connection);
            statement.setInt(1, newId);
            statement.setString(2, name);
            statement.setString(3, countryCode);
            statement.executeUpdate();
            return new City(newId, name, countryCode);
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to insert city", ex);
        }
    }

    public List<City> search(String query, int limit) {
        String sql = "SELECT id, name_ru, country_code FROM cities WHERE LOWER(name_ru) LIKE LOWER(?) ORDER BY name_ru LIMIT ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "%" + query + "%");
            statement.setInt(2, limit);
            try (ResultSet rs = statement.executeQuery()) {
                List<City> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
                return result;
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to search cities", ex);
        }
    }

    private int generateNextId(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COALESCE(MAX(id), 0) + 1 FROM cities")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("Failed to generate city id");
        }
    }

    private City mapRow(ResultSet rs) throws SQLException {
        return new City(rs.getInt("id"), rs.getString("name_ru"), rs.getString("country_code"));
    }
}
