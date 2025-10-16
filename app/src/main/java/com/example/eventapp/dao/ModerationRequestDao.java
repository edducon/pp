package com.example.eventapp.dao;

import com.example.eventapp.model.Direction;
import com.example.eventapp.model.EventActivity;
import com.example.eventapp.model.FullName;
import com.example.eventapp.model.Gender;
import com.example.eventapp.model.ModerationRequest;
import com.example.eventapp.model.Moderator;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ModerationRequestDao {
    private final DataSource dataSource;
    public ModerationRequestDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ModerationRequest create(long activityId, long moderatorId, ModerationRequest.Status status, Long conflictActivityId, String response) {
        String sql = "INSERT INTO moderation_requests(activity_id, moderator_id, status, conflict_activity_id, response_message) VALUES(?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, activityId);
            statement.setLong(2, moderatorId);
            statement.setString(3, status.name());
            if (conflictActivityId == null) {
                statement.setNull(4, Types.BIGINT);
            } else {
                statement.setLong(4, conflictActivityId);
            }
            statement.setString(5, response);
            statement.executeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    return findById(id).orElseThrow();
                }
                throw new SQLException("Failed to create moderation request");
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to create moderation request", ex);
        }
    }

    public Optional<ModerationRequest> findById(long id) {
        String sql = baseQuery("WHERE mr.id = ?");
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
            throw new RuntimeException("Failed to load moderation request", ex);
        }
    }

    public List<ModerationRequest> findByModerator(long moderatorId) {
        String sql = baseQuery("WHERE mr.moderator_id = ? ORDER BY mr.created_at DESC");
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, moderatorId);
            try (ResultSet rs = statement.executeQuery()) {
                List<ModerationRequest> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
                return result;
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to list moderation requests", ex);
        }
    }

    public List<ModerationRequest> findByEvent(long eventId) {
        String sql = baseQuery("WHERE a.event_id = ? ORDER BY mr.created_at DESC");
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            try (ResultSet rs = statement.executeQuery()) {
                List<ModerationRequest> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
                return result;
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to list moderation requests for event", ex);
        }
    }

    public List<ModerationRequest> findPendingByOrganizer(long organizerId) {
        String sql = baseQuery("WHERE e.organizer_id = ? AND mr.status = 'PENDING'");
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, organizerId);
            try (ResultSet rs = statement.executeQuery()) {
                List<ModerationRequest> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
                return result;
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to load pending moderation requests", ex);
        }
    }

    public Optional<ModerationRequest> findActiveForModerator(long activityId, long moderatorId) {
        String sql = baseQuery("WHERE mr.activity_id = ? AND mr.moderator_id = ? AND mr.status IN ('PENDING','APPROVED')");
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, activityId);
            statement.setLong(2, moderatorId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to find moderation request", ex);
        }
    }

    public List<ModerationRequest> findConflicts(long moderatorId, LocalDateTime startTime, LocalDateTime endTime) {
        String sql = baseQuery("WHERE mr.moderator_id = ? AND mr.status IN ('PENDING','APPROVED') " +
                "AND ((a.start_time < ? AND a.end_time > ?) OR (a.start_time BETWEEN ? AND ?) OR (a.end_time BETWEEN ? AND ?))");
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, moderatorId);
            Timestamp start = Timestamp.valueOf(startTime);
            Timestamp end = Timestamp.valueOf(endTime);
            statement.setTimestamp(2, start);
            statement.setTimestamp(3, end);
            statement.setTimestamp(4, start);
            statement.setTimestamp(5, end);
            statement.setTimestamp(6, start);
            statement.setTimestamp(7, end);
            try (ResultSet rs = statement.executeQuery()) {
                List<ModerationRequest> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
                return result;
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to find conflicting requests", ex);
        }
    }

    public void updateStatus(long requestId, ModerationRequest.Status status, String responseMessage, String declineReason) {
        String sql = "UPDATE moderation_requests SET status = ?, response_message = ?, decline_reason = ? WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setString(2, responseMessage);
            statement.setString(3, declineReason);
            statement.setLong(4, requestId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update moderation request", ex);
        }
    }

    private String baseQuery(String whereClause) {
        return "SELECT mr.*, a.start_time as act_start, a.end_time as act_end, e.organizer_id, e.title as event_title, d.name as direction_name, " +
                "m.id as moderator_id, m.id_number as moderator_id_number, m.last_name as m_last, m.first_name as m_first, m.middle_name as m_middle, m.email as m_email, " +
                "m.birth_date as m_birth, m.country_code as m_country, m.city_id as m_city, m.phone as m_phone, m.gender as m_gender, m.photo_path as m_photo, dir.id as dir_id, dir.name as dir_name " +
                "FROM moderation_requests mr " +
                "JOIN event_activities a ON mr.activity_id = a.id " +
                "JOIN events e ON a.event_id = e.id " +
                "JOIN directions d ON e.direction_id = d.id " +
                "JOIN moderators m ON mr.moderator_id = m.id " +
                "LEFT JOIN directions dir ON m.direction_id = dir.id " +
                whereClause;
    }

    private ModerationRequest mapRow(ResultSet rs) throws SQLException {
        EventActivity activity = new EventActivity(rs.getLong("activity_id"), rs.getLong("event_id"),
                rs.getString("title"), rs.getString("description"),
                rs.getTimestamp("act_start").toLocalDateTime(), rs.getTimestamp("act_end").toLocalDateTime());
        Direction direction = null;
        Long directionId = rs.getObject("dir_id", Long.class);
        if (directionId != null) {
            direction = new Direction(directionId, rs.getString("dir_name"));
        }
        Moderator moderator = new Moderator(rs.getLong("moderator_id"), rs.getString("moderator_id_number"),
                new FullName(rs.getString("m_last"), rs.getString("m_first"), rs.getString("m_middle")),
                rs.getString("m_email"), rs.getDate("m_birth").toLocalDate(), rs.getString("m_country"),
                rs.getInt("m_city"), rs.getString("m_phone"), Gender.fromDatabase(rs.getString("m_gender")),
                rs.getString("m_photo"), direction);
        return new ModerationRequest(rs.getLong("id"), activity, moderator,
                ModerationRequest.Status.valueOf(rs.getString("status")),
                Optional.ofNullable(rs.getObject("conflict_activity_id", Long.class)).orElse(null),
                rs.getString("response_message"), rs.getString("decline_reason"),
                rs.getTimestamp("created_at").toLocalDateTime(), rs.getTimestamp("updated_at").toLocalDateTime());
    }
}
