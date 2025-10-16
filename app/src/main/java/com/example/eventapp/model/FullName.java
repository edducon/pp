package com.example.eventapp.model;

public class FullName {
    private final String lastName;
    private final String firstName;
    private final String middleName;

    public FullName(String lastName, String firstName, String middleName) {
        this.lastName = lastName;
        this.firstName = firstName;
        this.middleName = middleName;
    }

    public String lastName() {
        return lastName;
    }

    public String firstName() {
        return firstName;
    }

    public String middleName() {
        return middleName;
    }

    public String forGreeting() {
        return (firstName + " " + (middleName == null ? "" : middleName)).trim();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (lastName != null && !lastName.isBlank()) {
            builder.append(lastName).append(' ');
        }
        if (firstName != null && !firstName.isBlank()) {
            builder.append(firstName).append(' ');
        }
        if (middleName != null && !middleName.isBlank()) {
            builder.append(middleName);
        }
        return builder.toString().trim();
    }

    public static FullName fromSingleField(String value) {
        if (value == null || value.isBlank()) {
            return new FullName("", "", "");
        }
        String[] parts = value.trim().split("\\s+");
        return switch (parts.length) {
            case 1 -> new FullName(parts[0], "", "");
            case 2 -> new FullName(parts[0], parts[1], "");
            default -> new FullName(parts[0], parts[1], parts[2]);
        };
    }
}
