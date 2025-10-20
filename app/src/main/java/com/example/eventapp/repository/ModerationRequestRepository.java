package com.example.eventapp.repository;

import com.example.eventapp.domain.ModerationRequest;
import com.example.eventapp.domain.ModerationStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ModerationRequestRepository extends BaseRepository {

    public ModerationRequestRepository(DataSource dataSource) {
        super(dataSource);
    }

    public ModerationRequest create(long eventId, long organizerId, String message) {
        String sql = "INSERT INTO moderation_requests(event_id, organizer_id, status, message) VALUES (?,?,?,?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, eventId);
            statement.setLong(2, organizerId);
            statement.setString(3, ModerationStatus.PENDING.name());
            statement.setString(4, message);
            statement.executeUpdate();
            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                long id = keys.getLong(1);
                return new ModerationRequest(id, eventId, organizerId, null, ModerationStatus.PENDING, LocalDateTime.now(), null, message);
            }
            throw new IllegalStateException("Failed to create moderation request");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create moderation request", e);
        }
    }

    public List<ModerationRequest> findPending() {
        String sql = "SELECT id, event_id, organizer_id, moderator_id, status, created_at, updated_at, message FROM moderation_requests WHERE status = 'PENDING' ORDER BY created_at";
        return fetch(sql);
    }

    public List<ModerationRequest> findByModerator(long moderatorId) {
        String sql = "SELECT id, event_id, organizer_id, moderator_id, status, created_at, updated_at, message FROM moderation_requests WHERE moderator_id = ? ORDER BY created_at";
        return fetch(sql, moderatorId);
    }

    public List<ModerationRequest> findByOrganizer(long organizerId) {
        String sql = "SELECT id, event_id, organizer_id, moderator_id, status, created_at, updated_at, message FROM moderation_requests WHERE organizer_id = ? ORDER BY created_at";
        return fetch(sql, organizerId);
    }

    public void assignModerator(long requestId, long moderatorId) {
        String sql = "UPDATE moderation_requests SET moderator_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, moderatorId);
            statement.setLong(2, requestId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to assign moderator", e);
        }
    }

    public void updateStatus(long requestId, ModerationStatus status, String message) {
        String sql = "UPDATE moderation_requests SET status = ?, message = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setString(2, message);
            statement.setLong(3, requestId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update moderation request", e);
        }
    }

    private List<ModerationRequest> fetch(String sql, Object... params) {
        List<ModerationRequest> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                result.add(mapRequest(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load moderation requests", e);
        }
        return result;
    }

    private ModerationRequest mapRequest(ResultSet rs) throws SQLException {
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        return new ModerationRequest(
                rs.getLong("id"),
                rs.getLong("event_id"),
                rs.getLong("organizer_id"),
                rs.getObject("moderator_id") != null ? rs.getLong("moderator_id") : null,
                ModerationStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toLocalDateTime(),
                updatedAt != null ? updatedAt.toLocalDateTime() : null,
                rs.getString("message")
        );
    }
}
