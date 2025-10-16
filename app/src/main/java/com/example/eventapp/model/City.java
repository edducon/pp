package com.example.eventapp.model;

public class City {
    private final int id;
    private final String name;
    private final String countryCode;

    public City(int id, String name, String countryCode) {
        this.id = id;
        this.name = name;
        this.countryCode = countryCode;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCountryCode() {
        return countryCode;
    }

    @Override
    public String toString() {
        return name;
    }
}
