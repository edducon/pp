package com.example.eventapp.service;

import com.example.eventapp.dao.CityDao;
import com.example.eventapp.dao.DirectionDao;
import com.example.eventapp.dao.JuryMemberDao;
import com.example.eventapp.dao.ModeratorDao;
import com.example.eventapp.dao.OrganizerDao;
import com.example.eventapp.dao.ParticipantDao;
import com.example.eventapp.model.Direction;
import com.example.eventapp.model.FullName;
import com.example.eventapp.model.Gender;
import com.example.eventapp.model.JuryMember;
import com.example.eventapp.model.Moderator;
import com.example.eventapp.model.Organizer;
import com.example.eventapp.model.Participant;
import com.example.eventapp.util.PasswordHasher;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class RegistrationService {
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[\\p{Punct}]).{6,}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrganizerDao organizerDao;
    private final ParticipantDao participantDao;
    private final ModeratorDao moderatorDao;
    private final JuryMemberDao juryMemberDao;
    private final DirectionDao directionDao;
    private final CityDao cityDao;

    public RegistrationService(OrganizerDao organizerDao, ParticipantDao participantDao,
                               ModeratorDao moderatorDao, JuryMemberDao juryMemberDao,
                               DirectionDao directionDao, CityDao cityDao) {
        this.organizerDao = organizerDao;
        this.participantDao = participantDao;
        this.moderatorDao = moderatorDao;
        this.juryMemberDao = juryMemberDao;
        this.directionDao = directionDao;
        this.cityDao = cityDao;
    }

    public String generateId(String prefix) {
        String normalizedPrefix = prefix.toUpperCase(Locale.ROOT);
        while (true) {
            String candidate = "%s-%04d".formatted(normalizedPrefix, RANDOM.nextInt(10_000));
            if (normalizedPrefix.startsWith("ORG") && organizerDao.findByIdNumber(candidate).isEmpty()) {
                return candidate;
            }
            if (normalizedPrefix.startsWith("PAR") && participantDao.findByIdNumber(candidate).isEmpty()) {
                return candidate;
            }
            if (normalizedPrefix.startsWith("MOD") && moderatorDao.findByIdNumber(candidate).isEmpty()) {
                return candidate;
            }
            if (normalizedPrefix.startsWith("JURY") && juryMemberDao.findByIdNumber(candidate).isEmpty()) {
                return candidate;
            }
        }
    }

    public boolean isPasswordValid(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    public Participant registerParticipant(String idNumber, FullName fullName, String email, LocalDate birthDate,
                                           String countryCode, int cityId, String phone, Gender gender, String photoPath,
                                           String rawPassword) {
        String hash = PasswordHasher.hash(rawPassword);
        Participant participant = new Participant(0, idNumber, fullName, email, birthDate, countryCode, cityId, phone, gender, photoPath);
        return participantDao.insert(participant, hash);
    }

    public Moderator registerModerator(String idNumber, FullName fullName, String email, LocalDate birthDate,
                                       String countryCode, int cityId, String phone, Gender gender, String photoPath,
                                       String rawPassword, String directionName) {
        Direction direction = directionName == null || directionName.isBlank() ? null : directionDao.getOrCreate(directionName);
        Moderator moderator = new Moderator(0, idNumber, fullName, email, birthDate, countryCode, cityId, phone, gender, photoPath, direction);
        return moderatorDao.insert(moderator, PasswordHasher.hash(rawPassword), direction == null ? null : direction.getId());
    }

    public JuryMember registerJury(String idNumber, FullName fullName, String email, LocalDate birthDate,
                                   String countryCode, int cityId, String phone, Gender gender, String photoPath,
                                   String rawPassword, String directionName) {
        Direction direction = directionName == null || directionName.isBlank() ? null : directionDao.getOrCreate(directionName);
        JuryMember member = new JuryMember(0, idNumber, fullName, email, birthDate, countryCode, cityId, phone, gender, photoPath, direction);
        return juryMemberDao.insert(member, PasswordHasher.hash(rawPassword), direction == null ? null : direction.getId());
    }

    public Optional<Direction> findDirectionByName(String name) {
        return directionDao.findByName(name);
    }
}
