package com.example.eventapp.service;

import com.example.eventapp.domain.JuryMemberProfile;
import com.example.eventapp.domain.ModeratorSummary;
import com.example.eventapp.domain.ParticipantProfile;
import com.example.eventapp.repository.AccountRepository;

import java.util.List;

public class DirectoryService {
    private final AccountRepository accountRepository;

    public DirectoryService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public List<ParticipantProfile> participants() {
        return accountRepository.findAllParticipants();
    }

    public List<ModeratorSummary> moderators() {
        return accountRepository.findAllModerators();
    }

    public List<JuryMemberProfile> juryMembers() {
        return accountRepository.findAllJuryMembers();
    }
}
