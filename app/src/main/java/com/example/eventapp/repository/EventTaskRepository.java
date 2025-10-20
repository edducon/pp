package com.example.eventapp.repository;

import com.example.eventapp.domain.EventTask;
import com.example.eventapp.domain.TaskStatus;

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

public class EventTaskRepository extends BaseRepository {

    public EventTaskRepository(DataSource dataSource) {
        super(dataSource);
    }

    public EventTask create(long eventId, String stage, String title, TaskStatus status, LocalDate dueDate, String assignee, String notes) {
        String sql = "INSERT INTO event_tasks(event_id, stage, title, status, due_date, assignee, notes) VALUES (?,?,?,?,?,?,?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, eventId);
            statement.setString(2, stage);
            statement.setString(3, title);
            statement.setString(4, status.name());
            if (dueDate != null) {
                statement.setDate(5, Date.valueOf(dueDate));
            } else {
                statement.setNull(5, java.sql.Types.DATE);
            }
            statement.setString(6, assignee);
            statement.setString(7, notes);
            statement.executeUpdate();
            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                long id = keys.getLong(1);
                return new EventTask(id, eventId, stage, title, status, dueDate, assignee, notes);
            }
            throw new IllegalStateException("Failed to create task");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create task", e);
        }
    }

    public List<EventTask> findByEvent(long eventId) {
        String sql = "SELECT id, event_id, stage, title, status, due_date, assignee, notes FROM event_tasks WHERE event_id = ? ORDER BY due_date";
        List<EventTask> tasks = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                tasks.add(mapTask(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch tasks", e);
        }
        return tasks;
    }

    public void updateStatus(long taskId, TaskStatus status) {
        String sql = "UPDATE event_tasks SET status = ? WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setLong(2, taskId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update task status", e);
        }
    }

    private EventTask mapTask(ResultSet rs) throws SQLException {
        Date due = rs.getDate("due_date");
        return new EventTask(
                rs.getLong("id"),
                rs.getLong("event_id"),
                rs.getString("stage"),
                rs.getString("title"),
                TaskStatus.valueOf(rs.getString("status")),
                due != null ? due.toLocalDate() : null,
                rs.getString("assignee"),
                rs.getString("notes")
        );
    }
}
