package com.example.eventapp.repository;

import com.example.eventapp.domain.City;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CityRepository extends BaseRepository {
    public CityRepository(DataSource dataSource) {
        super(dataSource);
    }

    public List<City> findAll() {
        String sql = "SELECT id, name FROM cities ORDER BY name";
        List<City> cities = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                cities.add(new City(rs.getLong("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch cities", e);
        }
        return cities;
    }
}
