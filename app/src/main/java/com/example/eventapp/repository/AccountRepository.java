package com.example.eventapp.repository;

import com.example.eventapp.domain.Account;
import com.example.eventapp.domain.JuryMemberProfile;
import com.example.eventapp.domain.ModeratorSummary;
import com.example.eventapp.domain.ParticipantProfile;
import com.example.eventapp.domain.Role;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AccountRepository extends BaseRepository {

    public AccountRepository(DataSource dataSource) {
        super(dataSource);
    }

    public Optional<Account> findByEmail(String email) {
        String sql = "SELECT id, email, password_hash, role, first_name, last_name, middle_name, phone, created_at FROM accounts WHERE email = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return Optional.of(mapAccount(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load account by email", e);
        }
    }

    public Optional<Account> findById(long id) {
        String sql = "SELECT id, email, password_hash, role, first_name, last_name, middle_name, phone, created_at FROM accounts WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return Optional.of(mapAccount(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load account by id", e);
        }
    }

    public Account createAccount(String email,
                                 String passwordHash,
                                 Role role,
                                 String firstName,
                                 String lastName,
                                 String middleName,
                                 String phone) {
        String sql = "INSERT INTO accounts(email, password_hash, role, first_name, last_name, middle_name, phone) VALUES (?,?,?,?,?,?,?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, email);
            statement.setString(2, passwordHash);
            statement.setString(3, role.name());
            statement.setString(4, firstName);
            statement.setString(5, lastName);
            statement.setString(6, middleName);
            statement.setString(7, phone);
            statement.executeUpdate();
            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                long id = keys.getLong(1);
                return new Account(id, email, passwordHash, role, firstName, lastName, middleName, phone, LocalDateTime.now());
            }
            throw new IllegalStateException("Failed to obtain generated id for account");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to insert account", e);
        }
    }

    public void createParticipantProfile(long accountId, String company, String jobTitle) {
        String sql = "INSERT INTO participants(account_id, company, job_title) VALUES (?,?,?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setString(2, company);
            statement.setString(3, jobTitle);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create participant profile", e);
        }
    }

    public void createOrganizerProfile(long accountId, String company, String website) {
        String sql = "INSERT INTO organizers(account_id, company, website) VALUES (?,?,?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setString(2, company);
            statement.setString(3, website);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create organizer profile", e);
        }
    }

    public void createModeratorProfile(long accountId, String expertise) {
        String sql = "INSERT INTO moderators(account_id, expertise) VALUES (?,?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setString(2, expertise);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create moderator profile", e);
        }
    }

    public void createJuryProfile(long accountId, String achievements) {
        String sql = "INSERT INTO jury_members(account_id, achievements) VALUES (?,?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setString(2, achievements);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create jury member profile", e);
        }
    }

    public List<ParticipantProfile> findAllParticipants() {
        String sql = "SELECT a.id, CONCAT_WS(' ', a.last_name, a.first_name, a.middle_name) AS full_name, p.company, p.job_title, a.email, a.phone " +
                "FROM participants p JOIN accounts a ON a.id = p.account_id ORDER BY full_name";
        List<ParticipantProfile> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                result.add(new ParticipantProfile(
                        rs.getLong("id"),
                        rs.getString("full_name"),
                        rs.getString("company"),
                        rs.getString("job_title"),
                        rs.getString("email"),
                        rs.getString("phone")
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch participants", e);
        }
        return result;
    }

    public List<ModeratorSummary> findAllModerators() {
        String sql = "SELECT a.id, CONCAT_WS(' ', a.last_name, a.first_name, a.middle_name) AS full_name, m.expertise, a.email " +
                "FROM moderators m JOIN accounts a ON a.id = m.account_id ORDER BY full_name";
        List<ModeratorSummary> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                result.add(new ModeratorSummary(
                        rs.getLong("id"),
                        rs.getString("full_name"),
                        rs.getString("expertise"),
                        rs.getString("email")
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch moderators", e);
        }
        return result;
    }

    public List<JuryMemberProfile> findAllJuryMembers() {
        String sql = "SELECT a.id, CONCAT_WS(' ', a.last_name, a.first_name, a.middle_name) AS full_name, j.achievements, a.email " +
                "FROM jury_members j JOIN accounts a ON a.id = j.account_id ORDER BY full_name";
        List<JuryMemberProfile> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                result.add(new JuryMemberProfile(
                        rs.getLong("id"),
                        rs.getString("full_name"),
                        rs.getString("achievements"),
                        rs.getString("email")
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch jury members", e);
        }
        return result;
    }

    private Account mapAccount(ResultSet rs) throws SQLException {
        return new Account(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("password_hash"),
                Role.valueOf(rs.getString("role")),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("middle_name"),
                rs.getString("phone"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
