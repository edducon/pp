package com.example.eventapp.service;

import com.example.eventapp.dao.JuryMemberDao;
import com.example.eventapp.dao.ModeratorDao;
import com.example.eventapp.dao.OrganizerDao;
import com.example.eventapp.dao.ParticipantDao;
import com.example.eventapp.model.AuthenticatedUser;
import com.example.eventapp.model.JuryMember;
import com.example.eventapp.model.Moderator;
import com.example.eventapp.model.Organizer;
import com.example.eventapp.model.Participant;
import com.example.eventapp.model.UserAccount;
import com.example.eventapp.util.PasswordHasher;

import java.util.Optional;

public class AuthenticationService {
    private final OrganizerDao organizerDao;
    private final ParticipantDao participantDao;
    private final ModeratorDao moderatorDao;
    private final JuryMemberDao juryMemberDao;

    public AuthenticationService(OrganizerDao organizerDao, ParticipantDao participantDao,
                                 ModeratorDao moderatorDao, JuryMemberDao juryMemberDao) {
        this.organizerDao = organizerDao;
        this.participantDao = participantDao;
        this.moderatorDao = moderatorDao;
        this.juryMemberDao = juryMemberDao;
    }

    public Optional<AuthenticatedUser> authenticate(String idNumber, String password) {
        Optional<Organizer> organizer = organizerDao.findByIdNumber(idNumber);
        if (organizer.isPresent() && passwordMatches(password, organizerDao.findPasswordHash(idNumber))) {
            return Optional.of(new AuthenticatedUser(organizer.get()));
        }
        Optional<Participant> participant = participantDao.findByIdNumber(idNumber);
        if (participant.isPresent() && passwordMatches(password, participantDao.findPasswordHash(idNumber))) {
            return Optional.of(new AuthenticatedUser(participant.get()));
        }
        Optional<Moderator> moderator = moderatorDao.findByIdNumber(idNumber);
        if (moderator.isPresent() && passwordMatches(password, moderatorDao.findPasswordHash(idNumber))) {
            return Optional.of(new AuthenticatedUser(moderator.get()));
        }
        Optional<JuryMember> jury = juryMemberDao.findByIdNumber(idNumber);
        if (jury.isPresent() && passwordMatches(password, juryMemberDao.findPasswordHash(idNumber))) {
            return Optional.of(new AuthenticatedUser(jury.get()));
        }
        return Optional.empty();
    }

    private boolean passwordMatches(String rawPassword, Optional<String> hash) {
        return hash.filter(stored -> PasswordHasher.matches(rawPassword, stored)).isPresent();
    }

    public UserAccount reloadAccount(AuthenticatedUser user) {
        return switch (user.getRole()) {
            case ORGANIZER -> organizerDao.findById(user.getAccount().getId()).map(Organizer.class::cast).orElseThrow();
            case PARTICIPANT -> participantDao.findByIdNumber(user.getAccount().getIdNumber()).map(Participant.class::cast).orElseThrow();
            case MODERATOR -> moderatorDao.findById(user.getAccount().getId()).map(Moderator.class::cast).orElseThrow();
            case JURY -> juryMemberDao.findByIdNumber(user.getAccount().getIdNumber()).map(JuryMember.class::cast).orElseThrow();
        };
    }
}
