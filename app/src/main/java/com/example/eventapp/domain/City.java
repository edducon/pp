package com.example.eventapp.domain;

public record City(long id, String name) {
    @Override
    public String toString() {
        return name;
    }
}
