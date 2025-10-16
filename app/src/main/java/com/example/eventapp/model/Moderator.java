package com.example.eventapp.model;

import java.time.LocalDate;

public class Moderator extends UserAccount {
    private final Direction direction;

    public Moderator(long id, String idNumber, FullName fullName, String email, LocalDate birthDate,
                     String countryCode, int cityId, String phone, Gender gender, String photoPath, Direction direction) {
        super(id, idNumber, fullName, email, birthDate, countryCode, cityId, phone, gender, photoPath, Role.MODERATOR);
        this.direction = direction;
    }

    public Direction getDirection() {
        return direction;
    }
}
