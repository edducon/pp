package com.example.eventapp.dao;

import com.example.eventapp.model.FullName;
import com.example.eventapp.model.Gender;
import com.example.eventapp.model.Participant;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ParticipantDao {
    private final DataSource dataSource;

    public ParticipantDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<Participant> findByIdNumber(String idNumber) {
        String sql = "SELECT * FROM participants WHERE id_number = ?";
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
            throw new RuntimeException("Failed to load participant", ex);
        }
    }

    public List<Participant> findAll() {
        String sql = "SELECT * FROM participants ORDER BY last_name";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<Participant> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to list participants", ex);
        }
    }

    public List<Participant> search(String lastNamePrefix, Long eventId) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT p.* FROM participants p ");
        List<Object> params = new ArrayList<>();
        if (eventId != null) {
            sql.append("JOIN activity_participants ap ON ap.participant_id = p.id " +
                    "JOIN event_activities a ON ap.activity_id = a.id WHERE a.event_id = ?");
            params.add(eventId);
        } else {
            sql.append("WHERE 1 = 1");
        }
        if (lastNamePrefix != null && !lastNamePrefix.isBlank()) {
            sql.append(" AND LOWER(p.last_name) LIKE LOWER(?)");
            params.add(lastNamePrefix + "%");
        }
        sql.append(" ORDER BY p.last_name");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = prepareStatement(connection, sql.toString(), params);
             ResultSet rs = statement.executeQuery()) {
            List<Participant> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to search participants", ex);
        }
    }

    public Optional<String> findPasswordHash(String idNumber) {
        String sql = "SELECT password_hash FROM participants WHERE id_number = ?";
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

    public Participant insert(Participant participant, String passwordHash) {
        String sql = "INSERT INTO participants(id_number, last_name, first_name, middle_name, email, birth_date, country_code, city_id, phone, password_hash, photo_path, gender) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, participant.getIdNumber());
            statement.setString(2, participant.getFullName().lastName());
            statement.setString(3, participant.getFullName().firstName());
            statement.setString(4, participant.getFullName().middleName());
            statement.setString(5, participant.getEmail());
            statement.setDate(6, Date.valueOf(participant.getBirthDate()));
            statement.setString(7, participant.getCountryCode());
            statement.setInt(8, participant.getCityId());
            statement.setString(9, participant.getPhone());
            statement.setString(10, passwordHash);
            statement.setString(11, participant.getPhotoPath());
            statement.setString(12, participant.getGender().toDatabase());
            statement.executeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    return new Participant(id, participant.getIdNumber(), participant.getFullName(), participant.getEmail(),
                            participant.getBirthDate(), participant.getCountryCode(), participant.getCityId(),
                            participant.getPhone(), participant.getGender(), participant.getPhotoPath());
                }
                throw new SQLException("Failed to read generated participant id");
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to insert participant", ex);
        }
    }

    private Participant mapRow(ResultSet rs) throws SQLException {
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
        return new Participant(id, idNumber, fullName, email, birthDate, countryCode, cityId, phone, gender, photoPath);
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
