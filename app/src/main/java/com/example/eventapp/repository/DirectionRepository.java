package com.example.eventapp.repository;

import com.example.eventapp.domain.Direction;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DirectionRepository extends BaseRepository {
    public DirectionRepository(DataSource dataSource) {
        super(dataSource);
    }

    public List<Direction> findAll() {
        String sql = "SELECT id, title FROM directions ORDER BY title";
        List<Direction> directions = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                directions.add(new Direction(rs.getLong("id"), rs.getString("title")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch directions", e);
        }
        return directions;
    }
}
