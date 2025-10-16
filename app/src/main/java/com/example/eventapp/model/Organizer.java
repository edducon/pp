package com.example.eventapp.model;

import java.time.LocalDate;

public class Organizer extends UserAccount {
    public Organizer(long id, String idNumber, FullName fullName, String email, LocalDate birthDate,
                     String countryCode, int cityId, String phone, Gender gender, String photoPath) {
        super(id, idNumber, fullName, email, birthDate, countryCode, cityId, phone, gender, photoPath, Role.ORGANIZER);
    }
}
