package com.example.eventapp.service;

import com.example.eventapp.dao.CityDao;
import com.example.eventapp.dao.DirectionDao;
import com.example.eventapp.dao.EventDao;
import com.example.eventapp.dao.ModerationRequestDao;
import com.example.eventapp.model.City;
import com.example.eventapp.model.Direction;
import com.example.eventapp.model.Event;
import com.example.eventapp.model.EventActivity;
import com.example.eventapp.model.ModerationRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class EventService {
    public static final Duration ACTIVITY_DURATION = Duration.ofMinutes(90);
    public static final Duration BREAK_DURATION = Duration.ofMinutes(15);

    private final EventDao eventDao;
    private final DirectionDao directionDao;
    private final CityDao cityDao;
    private final ModerationRequestDao moderationRequestDao;

    public EventService(EventDao eventDao, DirectionDao directionDao, CityDao cityDao, ModerationRequestDao moderationRequestDao) {
        this.eventDao = eventDao;
        this.directionDao = directionDao;
        this.cityDao = cityDao;
        this.moderationRequestDao = moderationRequestDao;
    }

    public List<Event> loadMainEvents(String direction, LocalDate date) {
        return eventDao.findForMainScreen(direction, date);
    }

    public Optional<Event> loadEvent(long eventId) {
        return eventDao.findDetailed(eventId);
    }

    public List<Event> loadOrganizerEvents(long organizerId) {
        return eventDao.findByOrganizer(organizerId);
    }

    public Set<Long> findEventsWithPendingRequests(long organizerId) {
        return moderationRequestDao.findPendingByOrganizer(organizerId).stream()
                .map(req -> req.getActivity().getEventId())
                .collect(Collectors.toSet());
    }

    public List<String> suggestTitles(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return eventDao.findSimilarTitles(query, 10);
    }

    public List<String> suggestDirections(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return directionDao.findSimilarNames(query, 10);
    }

    public Direction ensureDirection(String name) {
        return directionDao.getOrCreate(name);
    }

    public City ensureCity(String name, String countryCode) {
        return cityDao.findByName(name).orElseGet(() -> cityDao.insertCity(name, countryCode));
    }

    public Event createEvent(Event event, List<EventActivity> activities, Map<String, List<String>> juryByActivity) {
        return eventDao.createEvent(event, activities, juryByActivity);
    }

    public List<TimeSlot> calculateAvailableSlots(LocalDateTime eventStart, LocalDateTime eventEnd, List<EventActivity> scheduledActivities) {
        List<TimeSlot> available = new ArrayList<>();
        List<EventActivity> sortedActivities = new ArrayList<>(scheduledActivities);
        sortedActivities.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));
        LocalDateTime cursor = eventStart;
        for (EventActivity activity : sortedActivities) {
            if (cursor.isBefore(activity.getStartTime())) {
                fillSlots(available, cursor, activity.getStartTime());
            }
            cursor = activity.getEndTime().plus(BREAK_DURATION);
        }
        if (cursor.isBefore(eventEnd) || cursor.equals(eventEnd)) {
            fillSlots(available, cursor, eventEnd);
        }
        return available;
    }

    private void fillSlots(List<TimeSlot> slots, LocalDateTime slotStart, LocalDateTime slotEnd) {
        LocalDateTime cursor = slotStart;
        while (cursor.plus(ACTIVITY_DURATION).isBefore(slotEnd) || cursor.plus(ACTIVITY_DURATION).equals(slotEnd)) {
            LocalDateTime potentialEnd = cursor.plus(ACTIVITY_DURATION);
            if (potentialEnd.isAfter(slotEnd)) {
                break;
            }
            slots.add(new TimeSlot(cursor, potentialEnd));
            cursor = potentialEnd.plus(BREAK_DURATION);
        }
    }

    public record TimeSlot(LocalDateTime start, LocalDateTime end) {
        @Override
        public String toString() {
            return "%s - %s".formatted(start.toLocalTime(), end.toLocalTime());
        }
    }
}
