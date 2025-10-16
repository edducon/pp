package com.example.eventapp.util;

import java.time.LocalTime;

public final class TimeOfDayGreeting {
    private TimeOfDayGreeting() {
    }

    public static String forTime(LocalTime time) {
        if (time.isBefore(LocalTime.of(11, 1))) {
            return "Утро";
        }
        if (time.isBefore(LocalTime.of(18, 1))) {
            return "День";
        }
        return "Вечер";
    }
}
