package com.example.eventapp.model;

public class AuthenticatedUser {
    private final UserAccount account;

    public AuthenticatedUser(UserAccount account) {
        this.account = account;
    }

    public UserAccount getAccount() {
        return account;
    }

    public Role getRole() {
        return account.getRole();
    }
}
