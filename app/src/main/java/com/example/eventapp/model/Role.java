package com.example.eventapp.model;

public enum Role {
    ORGANIZER("Организатор"),
    MODERATOR("Модератор"),
    JURY("Жюри"),
    PARTICIPANT("Участник");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
