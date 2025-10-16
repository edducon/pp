package com.example.eventapp.dao;

import com.example.eventapp.model.*;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class EventDao {
    private final DataSource dataSource;

    public EventDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Event> findForMainScreen(String directionFilter, LocalDate dateFilter) {
        StringBuilder sql = new StringBuilder("SELECT e.*, d.id as d_id, d.name as d_name FROM events e " +
                "JOIN directions d ON e.direction_id = d.id WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (directionFilter != null && !directionFilter.isBlank()) {
            sql.append(" AND LOWER(d.name) LIKE LOWER(?)");
            params.add("%" + directionFilter + "%");
        }
        if (dateFilter != null) {
            sql.append(" AND DATE(e.start_time) <= ? AND DATE(e.end_time) >= ?");
            params.add(Date.valueOf(dateFilter));
            params.add(Date.valueOf(dateFilter));
        }
        sql.append(" ORDER BY e.start_time");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = prepareStatement(connection, sql.toString(), params);
             ResultSet rs = statement.executeQuery()) {
            List<Event> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapEvent(rs));
            }
            return result;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to load events", ex);
        }
    }

    public List<Event> findByOrganizer(long organizerId) {
        String sql = "SELECT e.*, d.id as d_id, d.name as d_name FROM events e " +
                "JOIN directions d ON e.direction_id = d.id WHERE e.organizer_id = ? ORDER BY e.start_time DESC";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, organizerId);
            try (ResultSet rs = statement.executeQuery()) {
                List<Event> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapEvent(rs));
                }
                return result;
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to load organizer events", ex);
        }
    }

    public Optional<Event> findDetailed(long eventId) {
        String sql = "SELECT e.*, d.id as d_id, d.name as d_name FROM events e " +
                "JOIN directions d ON e.direction_id = d.id WHERE e.id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Event event = mapEvent(rs);
                event.getActivities().addAll(loadActivitiesWithDetails(connection, event.getId()));
                return Optional.of(event);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to load event", ex);
        }
    }

    public List<String> findSimilarTitles(String term, int limit) {
        String sql = "SELECT DISTINCT title FROM events WHERE LOWER(title) LIKE LOWER(?) ORDER BY title LIMIT ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "%" + term + "%");
            statement.setInt(2, limit);
            try (ResultSet rs = statement.executeQuery()) {
                List<String> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(rs.getString("title"));
                }
                return result;
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to find similar titles", ex);
        }
    }

    public Event createEvent(Event event, List<EventActivity> activities, Map<String, List<String>> activityJuryByIdNumber) {
        String insertEventSql = "INSERT INTO events(organizer_id, title, direction_id, description, logo_path, city_id, start_time, end_time) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement stmt = connection.prepareStatement(insertEventSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, event.getOrganizerId());
                stmt.setString(2, event.getTitle());
                stmt.setLong(3, event.getDirection().getId());
                stmt.setString(4, event.getDescription());
                stmt.setString(5, event.getLogoPath());
                stmt.setInt(6, event.getCityId());
                stmt.setTimestamp(7, Timestamp.valueOf(event.getStartTime()));
                stmt.setTimestamp(8, Timestamp.valueOf(event.getEndTime()));
                stmt.executeUpdate();
                long eventId;
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SQLException("No generated key for event");
                    }
                    eventId = rs.getLong(1);
                }
                Map<Long, Long> activityIdMapping = new HashMap<>();
                for (EventActivity activity : activities) {
                    long activityId = insertActivity(connection, eventId, activity);
                    activityIdMapping.put(activity.getId(), activityId);
                }
                insertActivityJuryLinks(connection, activityIdMapping, activityJuryByIdNumber);
                connection.commit();
                return findDetailed(eventId).orElseThrow();
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to create event", ex);
        }
    }

    public List<EventActivity> loadActivitiesWithDetails(Connection connection, long eventId) throws SQLException {
        String sql = "SELECT a.id, a.event_id, a.title, a.description, a.start_time, a.end_time FROM event_activities a WHERE a.event_id = ? ORDER BY a.start_time";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            try (ResultSet rs = statement.executeQuery()) {
                List<EventActivity> activities = new ArrayList<>();
                while (rs.next()) {
                    EventActivity activity = mapActivity(rs);
                    activity.getJuryMembers().addAll(loadJuryForActivity(connection, activity.getId()));
                    activity.getTasks().addAll(loadTasksForActivity(connection, activity.getId()));
                    activity.getResources().addAll(loadResourcesForActivity(connection, activity.getId()));
                    activities.add(activity);
                }
                return activities;
            }
        }
    }

    public List<EventActivity> findActivitiesByModerator(long moderatorId) {
        String sql = "SELECT a.id, a.event_id, a.title, a.description, a.start_time, a.end_time, e.title as event_title, d.id as direction_id, d.name as direction_name " +
                "FROM event_activities a JOIN events e ON a.event_id = e.id " +
                "JOIN directions d ON e.direction_id = d.id " +
                "JOIN moderation_requests mr ON mr.activity_id = a.id " +
                "WHERE mr.moderator_id = ? AND mr.status IN ('APPROVED', 'PENDING') ORDER BY a.start_time";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, moderatorId);
            try (ResultSet rs = statement.executeQuery()) {
                List<EventActivity> activities = new ArrayList<>();
                while (rs.next()) {
                    EventActivity activity = mapActivity(rs);
                    activities.add(activity);
                }
                return activities;
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to load moderator activities", ex);
        }
    }

    public List<EventActivity> findAllActivities() {
        String sql = "SELECT a.id, a.event_id, a.title, a.description, a.start_time, a.end_time, e.title as event_title, d.id as direction_id, d.name as direction_name " +
                "FROM event_activities a JOIN events e ON a.event_id = e.id JOIN directions d ON e.direction_id = d.id ORDER BY a.start_time";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<EventActivity> activities = new ArrayList<>();
            while (rs.next()) {
                activities.add(mapActivity(rs));
            }
            return activities;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to load activities", ex);
        }
    }

    private List<JuryMember> loadJuryForActivity(Connection connection, long activityId) throws SQLException {
        String sql = "SELECT j.*, d.id as d_id, d.name as d_name FROM event_activity_jury ej " +
                "JOIN jury_members j ON ej.jury_id = j.id " +
                "LEFT JOIN directions d ON j.direction_id = d.id WHERE ej.activity_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, activityId);
            try (ResultSet rs = statement.executeQuery()) {
                List<JuryMember> jury = new ArrayList<>();
                while (rs.next()) {
                    jury.add(new JuryMember(rs.getLong("id"), rs.getString("id_number"),
                            new FullName(rs.getString("last_name"), rs.getString("first_name"), rs.getString("middle_name")),
                            rs.getString("email"), rs.getDate("birth_date").toLocalDate(),
                            rs.getString("country_code"), rs.getInt("city_id"), rs.getString("phone"),
                            Gender.fromDatabase(rs.getString("gender")), rs.getString("photo_path"),
                            Optional.ofNullable(rs.getObject("d_id", Long.class)).map(id -> new Direction(id, rs.getString("d_name"))).orElse(null)));
                }
                return jury;
            }
        }
    }

    private List<ActivityTask> loadTasksForActivity(Connection connection, long activityId) throws SQLException {
        String sql = "SELECT t.*, p.id as participant_id, p.id_number as participant_id_number, p.last_name, p.first_name, p.middle_name, p.email, p.birth_date, p.country_code, p.city_id, p.phone, p.gender, p.photo_path " +
                "FROM activity_tasks t LEFT JOIN participants p ON t.participant_id = p.id WHERE t.activity_id = ? ORDER BY t.created_at";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, activityId);
            try (ResultSet rs = statement.executeQuery()) {
                List<ActivityTask> tasks = new ArrayList<>();
                while (rs.next()) {
                    Participant participant = null;
                    Long participantId = rs.getObject("participant_id", Long.class);
                    if (participantId != null) {
                        participant = new Participant(participantId, rs.getString("participant_id_number"),
                                new FullName(rs.getString("last_name"), rs.getString("first_name"), rs.getString("middle_name")),
                                rs.getString("email"), rs.getDate("birth_date").toLocalDate(), rs.getString("country_code"),
                                rs.getInt("city_id"), rs.getString("phone"), Gender.fromDatabase(rs.getString("gender")),
                                rs.getString("photo_path"));
                    }
                    tasks.add(new ActivityTask(rs.getLong("id"), activityId, rs.getString("title"),
                            rs.getString("description"), participant,
                            rs.getTimestamp("created_at").toLocalDateTime()));
                }
                return tasks;
            }
        }
    }

    private List<ActivityResource> loadResourcesForActivity(Connection connection, long activityId) throws SQLException {
        String sql = "SELECT r.*, m.id as moderator_id, m.id_number as moderator_id_number, m.last_name, m.first_name, m.middle_name, m.email, m.birth_date, m.country_code, m.city_id, m.phone, m.gender, m.photo_path, d.id as d_id, d.name as d_name " +
                "FROM activity_resources r LEFT JOIN moderators m ON r.uploaded_by_moderator = m.id " +
                "LEFT JOIN directions d ON m.direction_id = d.id WHERE r.activity_id = ? ORDER BY r.uploaded_at";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, activityId);
            try (ResultSet rs = statement.executeQuery()) {
                List<ActivityResource> resources = new ArrayList<>();
                while (rs.next()) {
                    Moderator moderator = null;
                    Long moderatorId = rs.getObject("moderator_id", Long.class);
                    if (moderatorId != null) {
                        moderator = new Moderator(moderatorId, rs.getString("moderator_id_number"),
                                new FullName(rs.getString("last_name"), rs.getString("first_name"), rs.getString("middle_name")),
                                rs.getString("email"), rs.getDate("birth_date").toLocalDate(), rs.getString("country_code"),
                                rs.getInt("city_id"), rs.getString("phone"), Gender.fromDatabase(rs.getString("gender")),
                                rs.getString("photo_path"),
                                Optional.ofNullable(rs.getObject("d_id", Long.class)).map(id -> new Direction(id, rs.getString("d_name"))).orElse(null));
                    }
                    resources.add(new ActivityResource(rs.getLong("id"), activityId, rs.getString("name"),
                            rs.getString("resource_path"), moderator,
                            rs.getTimestamp("uploaded_at").toLocalDateTime()));
                }
                return resources;
            }
        }
    }

    private long insertActivity(Connection connection, long eventId, EventActivity activity) throws SQLException {
        String sql = "INSERT INTO event_activities(event_id, title, description, start_time, end_time) VALUES(?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, eventId);
            statement.setString(2, activity.getTitle());
            statement.setString(3, activity.getDescription());
            statement.setTimestamp(4, Timestamp.valueOf(activity.getStartTime()));
            statement.setTimestamp(5, Timestamp.valueOf(activity.getEndTime()));
            statement.executeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Failed to insert activity");
            }
        }
    }

    private void insertActivityJuryLinks(Connection connection, Map<Long, Long> activityIdMapping, Map<String, List<String>> activityJuryByIdNumber) throws SQLException {
        if (activityJuryByIdNumber == null || activityJuryByIdNumber.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO event_activity_jury(activity_id, jury_id) VALUES(?, (SELECT id FROM jury_members WHERE id_number = ?))";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Map.Entry<String, List<String>> entry : activityJuryByIdNumber.entrySet()) {
                Long tempId = Long.valueOf(entry.getKey());
                Long actualId = activityIdMapping.get(tempId);
                if (actualId == null) {
                    continue;
                }
                for (String juryIdNumber : entry.getValue()) {
                    statement.setLong(1, actualId);
                    statement.setString(2, juryIdNumber);
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        }
    }

    private PreparedStatement prepareStatement(Connection connection, String sql, List<Object> params) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        int index = 1;
        for (Object param : params) {
            if (param instanceof String s) {
                statement.setString(index++, s);
            } else if (param instanceof Integer i) {
                statement.setInt(index++, i);
            } else if (param instanceof Long l) {
                statement.setLong(index++, l);
            } else if (param instanceof Date d) {
                statement.setDate(index++, d);
            } else {
                statement.setObject(index++, param);
            }
        }
        return statement;
    }

    private Event mapEvent(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        long organizerId = rs.getLong("organizer_id");
        String title = rs.getString("title");
        Direction direction = new Direction(rs.getLong("d_id"), rs.getString("d_name"));
        String description = rs.getString("description");
        String logoPath = rs.getString("logo_path");
        int cityId = rs.getInt("city_id");
        LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
        LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();
        return new Event(id, organizerId, title, direction, description, logoPath, cityId, startTime, endTime);
    }

    private EventActivity mapActivity(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        long eventId = rs.getLong("event_id");
        String title = rs.getString("title");
        String description = rs.getString("description");
        LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
        LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();
        EventActivity activity = new EventActivity(id, eventId, title, description, startTime, endTime);
        try {
            String eventTitle = rs.getString("event_title");
            if (eventTitle != null) {
                activity.setEventTitle(eventTitle);
            }
        } catch (SQLException ignored) {
        }
        try {
            Long dirId = rs.getObject("direction_id", Long.class);
            if (dirId != null) {
                activity.setEventDirection(new Direction(dirId, rs.getString("direction_name")));
            }
        } catch (SQLException ignored) {
        }
        return activity;
    }
}
