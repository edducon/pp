package com.example.eventapp.dao;

import com.example.eventapp.model.Direction;
import com.example.eventapp.model.FullName;
import com.example.eventapp.model.Gender;
import com.example.eventapp.model.JuryMember;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JuryMemberDao {
    private final DataSource dataSource;

    public JuryMemberDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<JuryMember> findByIdNumber(String idNumber) {
        String sql = "SELECT j.*, d.id as d_id, d.name as d_name FROM jury_members j " +
                "LEFT JOIN directions d ON j.direction_id = d.id WHERE j.id_number = ?";
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
            throw new RuntimeException("Failed to load jury member", ex);
        }
    }

    public List<JuryMember> findAll() {
        String sql = "SELECT j.*, d.id as d_id, d.name as d_name FROM jury_members j " +
                "LEFT JOIN directions d ON j.direction_id = d.id ORDER BY j.last_name";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<JuryMember> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to list jury members", ex);
        }
    }

    public List<JuryMember> search(String lastNamePrefix, Long eventId) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT j.*, d.id as d_id, d.name as d_name FROM jury_members j " +
                "LEFT JOIN directions d ON j.direction_id = d.id ");
        List<Object> params = new ArrayList<>();
        if (eventId != null) {
            sql.append("JOIN event_activity_jury ej ON ej.jury_id = j.id " +
                    "JOIN event_activities a ON ej.activity_id = a.id WHERE a.event_id = ?");
            params.add(eventId);
        } else {
            sql.append("WHERE 1 = 1");
        }
        if (lastNamePrefix != null && !lastNamePrefix.isBlank()) {
            sql.append(" AND LOWER(j.last_name) LIKE LOWER(?)");
            params.add(lastNamePrefix + "%");
        }
        sql.append(" ORDER BY j.last_name");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = prepareStatement(connection, sql.toString(), params);
             ResultSet rs = statement.executeQuery()) {
            List<JuryMember> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to search jury members", ex);
        }
    }

    public Optional<String> findPasswordHash(String idNumber) {
        String sql = "SELECT password_hash FROM jury_members WHERE id_number = ?";
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

    public JuryMember insert(JuryMember juryMember, String passwordHash, Long directionId) {
        String sql = "INSERT INTO jury_members(id_number, last_name, first_name, middle_name, email, birth_date, country_code, city_id, phone, direction_id, password_hash, photo_path, gender) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, juryMember.getIdNumber());
            statement.setString(2, juryMember.getFullName().lastName());
            statement.setString(3, juryMember.getFullName().firstName());
            statement.setString(4, juryMember.getFullName().middleName());
            statement.setString(5, juryMember.getEmail());
            statement.setDate(6, Date.valueOf(juryMember.getBirthDate()));
            statement.setString(7, juryMember.getCountryCode());
            statement.setInt(8, juryMember.getCityId());
            statement.setString(9, juryMember.getPhone());
            if (directionId == null) {
                statement.setNull(10, Types.BIGINT);
            } else {
                statement.setLong(10, directionId);
            }
            statement.setString(11, passwordHash);
            statement.setString(12, juryMember.getPhotoPath());
            statement.setString(13, juryMember.getGender().toDatabase());
            statement.executeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    return new JuryMember(id, juryMember.getIdNumber(), juryMember.getFullName(), juryMember.getEmail(),
                            juryMember.getBirthDate(), juryMember.getCountryCode(), juryMember.getCityId(),
                            juryMember.getPhone(), juryMember.getGender(), juryMember.getPhotoPath(), juryMember.getDirection());
                }
                throw new SQLException("Failed to read generated jury id");
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to insert jury member", ex);
        }
    }

    private JuryMember mapRow(ResultSet rs) throws SQLException {
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
        return new JuryMember(id, idNumber, fullName, email, birthDate, countryCode, cityId, phone, gender, photoPath, direction);
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
