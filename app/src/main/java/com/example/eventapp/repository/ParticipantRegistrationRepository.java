package com.example.eventapp.repository;

import com.example.eventapp.domain.ParticipantRegistration;
import com.example.eventapp.domain.RegistrationStatus;

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

public class ParticipantRegistrationRepository extends BaseRepository {

    public ParticipantRegistrationRepository(DataSource dataSource) {
        super(dataSource);
    }

    public ParticipantRegistration create(long eventId, long participantId) {
        String sql = "INSERT INTO participant_registrations(event_id, participant_id, status) VALUES (?,?,?) ON DUPLICATE KEY UPDATE status = VALUES(status), created_at = CURRENT_TIMESTAMP";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, eventId);
            statement.setLong(2, participantId);
            statement.setString(3, RegistrationStatus.REQUESTED.name());
            statement.executeUpdate();
            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                long id = keys.getLong(1);
                return new ParticipantRegistration(id, eventId, participantId, RegistrationStatus.REQUESTED, LocalDateTime.now());
            }
            // If duplicate key triggered, fetch existing row
            return findByEventAndParticipant(eventId, participantId);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create registration", e);
        }
    }

    public void updateStatus(long registrationId, RegistrationStatus status) {
        String sql = "UPDATE participant_registrations SET status = ?, created_at = created_at WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setLong(2, registrationId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update registration", e);
        }
    }

    public ParticipantRegistration findByEventAndParticipant(long eventId, long participantId) {
        String sql = "SELECT id, event_id, participant_id, status, created_at FROM participant_registrations WHERE event_id = ? AND participant_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            statement.setLong(2, participantId);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return map(rs);
            }
            throw new IllegalStateException("Registration not found");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch registration", e);
        }
    }

    public List<ParticipantRegistration> findByEvent(long eventId) {
        String sql = "SELECT id, event_id, participant_id, status, created_at FROM participant_registrations WHERE event_id = ? ORDER BY created_at";
        List<ParticipantRegistration> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                result.add(map(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch registrations", e);
        }
        return result;
    }

    private ParticipantRegistration map(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        return new ParticipantRegistration(
                rs.getLong("id"),
                rs.getLong("event_id"),
                rs.getLong("participant_id"),
                RegistrationStatus.valueOf(rs.getString("status")),
                created != null ? created.toLocalDateTime() : null
        );
    }
}
