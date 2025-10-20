package com.example.eventapp.repository;

import com.example.eventapp.domain.Event;
import com.example.eventapp.domain.EventDetails;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EventRepository extends BaseRepository {

    public EventRepository(DataSource dataSource) {
        super(dataSource);
    }

    public Event create(String title,
                        String description,
                        LocalDate startDate,
                        LocalDate endDate,
                        long cityId,
                        long directionId,
                        long organizerId,
                        int capacity) {
        String sql = "INSERT INTO events(title, description, start_date, end_date, city_id, direction_id, organizer_id, capacity, status) " +
                "VALUES (?,?,?,?,?,?,?,?, 'DRAFT')";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, title);
            statement.setString(2, description);
            statement.setDate(3, Date.valueOf(startDate));
            statement.setDate(4, Date.valueOf(endDate));
            statement.setLong(5, cityId);
            statement.setLong(6, directionId);
            statement.setLong(7, organizerId);
            statement.setInt(8, capacity);
            statement.executeUpdate();
            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                long id = keys.getLong(1);
                return new Event(id, title, description, startDate, endDate, cityId, directionId, organizerId, capacity, "DRAFT");
            }
            throw new IllegalStateException("Failed to obtain generated id for event");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create event", e);
        }
    }

    public void updateStatus(long eventId, String status) {
        String sql = "UPDATE events SET status = ? WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, eventId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update event status", e);
        }
    }

    public Optional<Event> findById(long id) {
        String sql = "SELECT id, title, description, start_date, end_date, city_id, direction_id, organizer_id, capacity, status FROM events WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return Optional.of(mapEvent(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load event", e);
        }
    }

    public List<EventDetails> findByOrganizer(long organizerId) {
        String sql = baseDetailsQuery() + " WHERE e.organizer_id = ? ORDER BY e.start_date";
        return fetchDetails(sql, organizerId);
    }

    public List<EventDetails> findUpcomingCatalog() {
        String sql = baseDetailsQuery() + " WHERE e.status IN ('PUBLISHED','APPROVED') ORDER BY e.start_date";
        return fetchDetails(sql);
    }

    public List<EventDetails> findAllForModeration() {
        String sql = baseDetailsQuery() + " ORDER BY e.start_date";
        return fetchDetails(sql);
    }

    public List<EventDetails> findAll() {
        String sql = baseDetailsQuery() + " ORDER BY e.start_date";
        return fetchDetails(sql);
    }

    private List<EventDetails> fetchDetails(String sql, Object... params) {
        List<EventDetails> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                result.add(mapDetails(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch events", e);
        }
        return result;
    }

    private Event mapEvent(ResultSet rs) throws SQLException {
        return new Event(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date").toLocalDate(),
                rs.getLong("city_id"),
                rs.getLong("direction_id"),
                rs.getLong("organizer_id"),
                rs.getInt("capacity"),
                rs.getString("status")
        );
    }

    private EventDetails mapDetails(ResultSet rs) throws SQLException {
        return new EventDetails(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date").toLocalDate(),
                rs.getString("city_name"),
                rs.getString("direction_name"),
                rs.getString("status"),
                rs.getInt("capacity"),
                rs.getLong("organizer_id")
        );
    }

    private String baseDetailsQuery() {
        return "SELECT e.id, e.title, e.description, e.start_date, e.end_date, c.name AS city_name, d.title AS direction_name, e.status, e.capacity, e.organizer_id " +
                "FROM events e " +
                "JOIN cities c ON c.id = e.city_id " +
                "JOIN directions d ON d.id = e.direction_id";
    }
}
