package com.example.eventapp.model;

public class Direction {
    private final long id;
    private final String name;

    public Direction(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
