package com.example.eventapp.dao;

import com.example.eventapp.model.Direction;
import com.example.eventapp.model.FullName;
import com.example.eventapp.model.Gender;
import com.example.eventapp.model.Moderator;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ModeratorDao {
    private final DataSource dataSource;

    public ModeratorDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<Moderator> findByIdNumber(String idNumber) {
        String sql = "SELECT m.*, d.id as d_id, d.name as d_name FROM moderators m " +
                "LEFT JOIN directions d ON m.direction_id = d.id WHERE m.id_number = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, idNumber);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to load moderator", ex);
        }
    }

    public Optional<Moderator> findById(long id) {
        String sql = "SELECT m.*, d.id as d_id, d.name as d_name FROM moderators m " +
                "LEFT JOIN directions d ON m.direction_id = d.id WHERE m.id = ?";
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
            throw new RuntimeException("Failed to load moderator", ex);
        }
    }

    public List<Moderator> findAll() {
        String sql = "SELECT m.*, d.id as d_id, d.name as d_name FROM moderators m " +
                "LEFT JOIN directions d ON m.direction_id = d.id ORDER BY m.last_name";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<Moderator> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to list moderators", ex);
        }
    }

    public List<Moderator> search(String lastNamePrefix, Long eventId) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT m.*, d.id as d_id, d.name as d_name FROM moderators m " +
                "LEFT JOIN directions d ON m.direction_id = d.id ");
        List<Object> params = new ArrayList<>();
        if (eventId != null) {
            sql.append("JOIN moderation_requests mr ON mr.moderator_id = m.id " +
                    "JOIN event_activities a ON mr.activity_id = a.id WHERE a.event_id = ?");
            params.add(eventId);
        } else {
            sql.append("WHERE 1 = 1");
        }
        if (lastNamePrefix != null && !lastNamePrefix.isBlank()) {
            sql.append(" AND LOWER(m.last_name) LIKE LOWER(?)");
            params.add(lastNamePrefix + "%");
        }
        sql.append(" ORDER BY m.last_name");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = prepareStatement(connection, sql.toString(), params);
             ResultSet rs = statement.executeQuery()) {
            List<Moderator> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to search moderators", ex);
        }
    }

    public Optional<String> findPasswordHash(String idNumber) {
        String sql = "SELECT password_hash FROM moderators WHERE id_number = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, idNumber);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("password_hash"));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to read password", ex);
        }
    }

    public Moderator insert(Moderator moderator, String passwordHash, Long directionId) {
        String sql = "INSERT INTO moderators(id_number, last_name, first_name, middle_name, email, birth_date, country_code, city_id, phone, direction_id, password_hash, photo_path, gender) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, moderator.getIdNumber());
            statement.setString(2, moderator.getFullName().lastName());
            statement.setString(3, moderator.getFullName().firstName());
            statement.setString(4, moderator.getFullName().middleName());
            statement.setString(5, moderator.getEmail());
            statement.setDate(6, Date.valueOf(moderator.getBirthDate()));
            statement.setString(7, moderator.getCountryCode());
            statement.setInt(8, moderator.getCityId());
            statement.setString(9, moderator.getPhone());
            if (directionId == null) {
                statement.setNull(10, Types.BIGINT);
            } else {
                statement.setLong(10, directionId);
            }
            statement.setString(11, passwordHash);
            statement.setString(12, moderator.getPhotoPath());
            statement.setString(13, moderator.getGender().toDatabase());
            statement.executeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    Direction direction = moderator.getDirection();
                    return new Moderator(id, moderator.getIdNumber(), moderator.getFullName(), moderator.getEmail(),
                            moderator.getBirthDate(), moderator.getCountryCode(), moderator.getCityId(),
                            moderator.getPhone(), moderator.getGender(), moderator.getPhotoPath(), direction);
                }
                throw new SQLException("Failed to read generated moderator id");
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to insert moderator", ex);
        }
    }

    public void updateProfile(Moderator moderator, String passwordHash, Long directionId) {
        StringBuilder sql = new StringBuilder("UPDATE moderators SET last_name = ?, first_name = ?, middle_name = ?, email = ?, " +
                "birth_date = ?, country_code = ?, city_id = ?, phone = ?, direction_id = ?, gender = ?, photo_path = ?");
        boolean updatePassword = passwordHash != null && !passwordHash.isBlank();
        if (updatePassword) {
            sql.append(", password_hash = ?");
        }
        sql.append(" WHERE id = ?");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setString(1, moderator.getFullName().lastName());
            statement.setString(2, moderator.getFullName().firstName());
            statement.setString(3, moderator.getFullName().middleName());
            statement.setString(4, moderator.getEmail());
            statement.setDate(5, Date.valueOf(moderator.getBirthDate()));
            statement.setString(6, moderator.getCountryCode());
            statement.setInt(7, moderator.getCityId());
            statement.setString(8, moderator.getPhone());
            if (directionId == null) {
                statement.setNull(9, Types.BIGINT);
            } else {
                statement.setLong(9, directionId);
            }
            statement.setString(10, moderator.getGender().toDatabase());
            statement.setString(11, moderator.getPhotoPath());
            int index = 12;
            if (updatePassword) {
                statement.setString(index++, passwordHash);
            }
            statement.setLong(index, moderator.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update moderator", ex);
        }
    }

    private Moderator mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String idNumber = rs.getString("id_number");
        FullName fullName = new FullName(rs.getString("last_name"), rs.getString("first_name"), rs.getString("middle_name"));
        String email = rs.getString("email");
        LocalDate birthDate = rs.getDate("birth_date").toLocalDate();
        String countryCode = rs.getString("country_code");
        int cityId = rs.getInt("city_id");
        String phone = rs.getString("phone");
        Gender gender = Gender.fromDatabase(rs.getString("gender"));
        String photoPath = rs.getString("photo_path");
        Long directionId = rs.getObject("d_id", Long.class);
        Direction direction = null;
        if (directionId != null) {
            direction = new Direction(directionId, rs.getString("d_name"));
        }
        return new Moderator(id, idNumber, fullName, email, birthDate, countryCode, cityId, phone, gender, photoPath, direction);
    }

    private PreparedStatement prepareStatement(Connection connection, String sql, List<Object> params) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        int index = 1;
        for (Object param : params) {
            if (param instanceof String s) {
                statement.setString(index++, s);
            } else if (param instanceof Long l) {
                statement.setLong(index++, l);
            } else {
                statement.setObject(index++, param);
            }
        }
        return statement;
    }
}
