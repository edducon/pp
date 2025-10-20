package com.example.eventapp.domain;

public record Direction(long id, String title) {
    @Override
    public String toString() {
        return title;
    }
}
