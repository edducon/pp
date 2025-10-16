package com.example.eventapp.dao;

import com.example.eventapp.model.ActivityResource;
import com.example.eventapp.model.ActivityTask;
import com.example.eventapp.model.Direction;
import com.example.eventapp.model.FullName;
import com.example.eventapp.model.Gender;
import com.example.eventapp.model.Moderator;
import com.example.eventapp.model.Participant;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class ActivityManagementDao {
    private final DataSource dataSource;

    public ActivityManagementDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ActivityTask addTask(long activityId, String title, String description, Long participantId) {
        String sql = "INSERT INTO activity_tasks(activity_id, title, description, participant_id, created_at) VALUES(?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, activityId);
            statement.setString(2, title);
            statement.setString(3, description);
            if (participantId == null) {
                statement.setNull(4, Types.BIGINT);
            } else {
                statement.setLong(4, participantId);
            }
            statement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            statement.executeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    return findTaskById(id);
                }
                throw new SQLException("Failed to create task");
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to add task", ex);
        }
    }

    public void deleteTask(long taskId) {
        String sql = "DELETE FROM activity_tasks WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, taskId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to delete task", ex);
        }
    }

    public ActivityResource addResource(long activityId, String name, String resourcePath, Long moderatorId) {
        String sql = "INSERT INTO activity_resources(activity_id, name, resource_path, uploaded_by_moderator, uploaded_at) VALUES(?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, activityId);
            statement.setString(2, name);
            statement.setString(3, resourcePath);
            if (moderatorId == null) {
                statement.setNull(4, Types.BIGINT);
            } else {
                statement.setLong(4, moderatorId);
            }
            statement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            statement.executeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    return findResourceById(id);
                }
                throw new SQLException("Failed to store resource");
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to add resource", ex);
        }
    }

    public void deleteResource(long resourceId) {
        String sql = "DELETE FROM activity_resources WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, resourceId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to delete resource", ex);
        }
    }

    private ActivityTask findTaskById(long id) throws SQLException {
        String sql = "SELECT t.*, p.id as participant_id, p.id_number, p.last_name, p.first_name, p.middle_name, p.email, p.birth_date, p.country_code, p.city_id, p.phone, p.gender, p.photo_path " +
                "FROM activity_tasks t LEFT JOIN participants p ON t.participant_id = p.id WHERE t.id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Task not found");
                }
                Participant participant = null;
                Long participantId = rs.getObject("participant_id", Long.class);
                if (participantId != null) {
                    participant = new Participant(participantId, rs.getString("id_number"),
                            new FullName(rs.getString("last_name"), rs.getString("first_name"), rs.getString("middle_name")),
                            rs.getString("email"), rs.getDate("birth_date").toLocalDate(), rs.getString("country_code"),
                            rs.getInt("city_id"), rs.getString("phone"), Gender.fromDatabase(rs.getString("gender")),
                            rs.getString("photo_path"));
                }
                return new ActivityTask(id, rs.getLong("activity_id"), rs.getString("title"),
                        rs.getString("description"), participant,
                        rs.getTimestamp("created_at").toLocalDateTime());
            }
        }
    }

    private ActivityResource findResourceById(long id) throws SQLException {
        String sql = "SELECT r.*, m.id as moderator_id, m.id_number, m.last_name, m.first_name, m.middle_name, m.email, m.birth_date, m.country_code, m.city_id, m.phone, m.gender, m.photo_path, d.id as dir_id, d.name as dir_name " +
                "FROM activity_resources r LEFT JOIN moderators m ON r.uploaded_by_moderator = m.id LEFT JOIN directions d ON m.direction_id = d.id WHERE r.id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Resource not found");
                }
                Moderator moderator = null;
                Long moderatorId = rs.getObject("moderator_id", Long.class);
                if (moderatorId != null) {
                    Long directionId = rs.getObject("dir_id", Long.class);
                    Direction direction = null;
                    if (directionId != null) {
                        direction = new Direction(directionId, rs.getString("dir_name"));
                    }
                    moderator = new Moderator(moderatorId, rs.getString("id_number"),
                            new FullName(rs.getString("last_name"), rs.getString("first_name"), rs.getString("middle_name")),
                            rs.getString("email"), rs.getDate("birth_date").toLocalDate(), rs.getString("country_code"),
                            rs.getInt("city_id"), rs.getString("phone"), Gender.fromDatabase(rs.getString("gender")),
                            rs.getString("photo_path"), direction);
                }
                return new ActivityResource(id, rs.getLong("activity_id"), rs.getString("name"),
                        rs.getString("resource_path"), moderator,
                        rs.getTimestamp("uploaded_at").toLocalDateTime());
            }
        }
    }
}
