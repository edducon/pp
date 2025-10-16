package com.example.eventapp.model;

import java.time.LocalDate;

public class UserAccount {
    private final long id;
    private final String idNumber;
    private final FullName fullName;
    private final String email;
    private final LocalDate birthDate;
    private final String countryCode;
    private final int cityId;
    private final String phone;
    private final Gender gender;
    private final String photoPath;
    private final Role role;

    public UserAccount(long id, String idNumber, FullName fullName, String email, LocalDate birthDate,
                       String countryCode, int cityId, String phone, Gender gender, String photoPath, Role role) {
        this.id = id;
        this.idNumber = idNumber;
        this.fullName = fullName;
        this.email = email;
        this.birthDate = birthDate;
        this.countryCode = countryCode;
        this.cityId = cityId;
        this.phone = phone;
        this.gender = gender;
        this.photoPath = photoPath;
        this.role = role;
    }

    public long getId() {
        return id;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public FullName getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public int getCityId() {
        return cityId;
    }

    public String getPhone() {
        return phone;
    }

    public Gender getGender() {
        return gender;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public Role getRole() {
        return role;
    }
}
