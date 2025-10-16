package com.example.eventapp.model;

public enum Gender {
    MALE("мужской"),
    FEMALE("женский"),
    OTHER("другое");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Gender fromDatabase(String value) {
        if (value == null) {
            return OTHER;
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "мужской", "male", "m" -> MALE;
            case "женский", "female", "f" -> FEMALE;
            default -> OTHER;
        };
    }

    public String toDatabase() {
        return displayName;
    }
}
