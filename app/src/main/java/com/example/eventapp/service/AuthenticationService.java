package com.example.eventapp.service;

import com.example.eventapp.domain.Account;
import com.example.eventapp.domain.AuthenticatedUser;
import com.example.eventapp.domain.Role;
import com.example.eventapp.repository.AccountRepository;
import com.example.eventapp.util.PasswordHasher;

import java.util.Optional;

public class AuthenticationService {
    private final AccountRepository accountRepository;

    public AuthenticationService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Optional<AuthenticatedUser> login(String email, String password) {
        return accountRepository.findByEmail(email)
                .filter(account -> PasswordHasher.matches(password, account.passwordHash()))
                .map(this::toAuthenticatedUser);
    }

    public AuthenticatedUser registerParticipant(String email,
                                                 String password,
                                                 String firstName,
                                                 String lastName,
                                                 String middleName,
                                                 String phone,
                                                 String company,
                                                 String jobTitle) {
        Account account = accountRepository.createAccount(email, PasswordHasher.hash(password), Role.PARTICIPANT,
                firstName, lastName, middleName, phone);
        accountRepository.createParticipantProfile(account.id(), company, jobTitle);
        return toAuthenticatedUser(account);
    }

    public AuthenticatedUser registerOrganizer(String email,
                                               String password,
                                               String firstName,
                                               String lastName,
                                               String middleName,
                                               String phone,
                                               String company,
                                               String website) {
        Account account = accountRepository.createAccount(email, PasswordHasher.hash(password), Role.ORGANIZER,
                firstName, lastName, middleName, phone);
        accountRepository.createOrganizerProfile(account.id(), company, website);
        return toAuthenticatedUser(account);
    }

    public AuthenticatedUser registerModerator(String email,
                                               String password,
                                               String firstName,
                                               String lastName,
                                               String middleName,
                                               String phone,
                                               String expertise) {
        Account account = accountRepository.createAccount(email, PasswordHasher.hash(password), Role.MODERATOR,
                firstName, lastName, middleName, phone);
        accountRepository.createModeratorProfile(account.id(), expertise);
        return toAuthenticatedUser(account);
    }

    public AuthenticatedUser registerJury(String email,
                                          String password,
                                          String firstName,
                                          String lastName,
                                          String middleName,
                                          String phone,
                                          String achievements) {
        Account account = accountRepository.createAccount(email, PasswordHasher.hash(password), Role.JURY,
                firstName, lastName, middleName, phone);
        accountRepository.createJuryProfile(account.id(), achievements);
        return toAuthenticatedUser(account);
    }

    private AuthenticatedUser toAuthenticatedUser(Account account) {
        return new AuthenticatedUser(account.id(), account.email(), account.fullName(), account.role());
    }
}
