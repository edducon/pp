package com.example.eventapp.dao;

import com.example.eventapp.model.FullName;
import com.example.eventapp.model.Gender;
import com.example.eventapp.model.Organizer;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;

public class OrganizerDao {
    private final DataSource dataSource;

    public OrganizerDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<Organizer> findById(long id) {
        String sql = "SELECT * FROM organizers WHERE id = ?";
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
            throw new RuntimeException("Failed to load organizer", ex);
        }
    }

    public Optional<Organizer> findByIdNumber(String idNumber) {
        String sql = "SELECT * FROM organizers WHERE id_number = ?";
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
            throw new RuntimeException("Failed to load organizer", ex);
        }
    }

    public Optional<String> findPasswordHash(String idNumber) {
        String sql = "SELECT password_hash FROM organizers WHERE id_number = ?";
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

    public void updateProfile(Organizer organizer, String passwordHash) {
        StringBuilder sql = new StringBuilder("UPDATE organizers SET last_name = ?, first_name = ?, middle_name = ?, email = ?, " +
                "birth_date = ?, country_code = ?, city_id = ?, phone = ?, gender = ?, photo_path = ?");
        boolean updatePassword = passwordHash != null && !passwordHash.isBlank();
        if (updatePassword) {
            sql.append(", password_hash = ?");
        }
        sql.append(" WHERE id = ?");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setString(1, organizer.getFullName().lastName());
            statement.setString(2, organizer.getFullName().firstName());
            statement.setString(3, organizer.getFullName().middleName());
            statement.setString(4, organizer.getEmail());
            statement.setDate(5, Date.valueOf(organizer.getBirthDate()));
            statement.setString(6, organizer.getCountryCode());
            statement.setInt(7, organizer.getCityId());
            statement.setString(8, organizer.getPhone());
            statement.setString(9, organizer.getGender().toDatabase());
            statement.setString(10, organizer.getPhotoPath());
            int parameterIndex = 11;
            if (updatePassword) {
                statement.setString(parameterIndex++, passwordHash);
            }
            statement.setLong(parameterIndex, organizer.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update organizer", ex);
        }
    }

    public Organizer insert(Organizer organizer, String passwordHash) {
        String sql = "INSERT INTO organizers(id_number, last_name, first_name, middle_name, email, birth_date, country_code, city_id, phone, password_hash, photo_path, gender) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, organizer.getIdNumber());
            statement.setString(2, organizer.getFullName().lastName());
            statement.setString(3, organizer.getFullName().firstName());
            statement.setString(4, organizer.getFullName().middleName());
            statement.setString(5, organizer.getEmail());
            statement.setDate(6, Date.valueOf(organizer.getBirthDate()));
            statement.setString(7, organizer.getCountryCode());
            statement.setInt(8, organizer.getCityId());
            statement.setString(9, organizer.getPhone());
            statement.setString(10, passwordHash);
            statement.setString(11, organizer.getPhotoPath());
            statement.setString(12, organizer.getGender().toDatabase());
            statement.executeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    return new Organizer(id, organizer.getIdNumber(), organizer.getFullName(), organizer.getEmail(),
                            organizer.getBirthDate(), organizer.getCountryCode(), organizer.getCityId(),
                            organizer.getPhone(), organizer.getGender(), organizer.getPhotoPath());
                }
                throw new SQLException("Failed to read generated organizer id");
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to insert organizer", ex);
        }
    }

    private Organizer mapRow(ResultSet rs) throws SQLException {
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
        return new Organizer(id, idNumber, fullName, email, birthDate, countryCode, cityId, phone, gender, photoPath);
    }
}
