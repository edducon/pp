package com.example.eventapp.service;

import com.example.eventapp.domain.Event;
import com.example.eventapp.domain.EventDetails;
import com.example.eventapp.domain.EventTask;
import com.example.eventapp.domain.TaskStatus;
import com.example.eventapp.repository.EventRepository;
import com.example.eventapp.repository.EventTaskRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class EventService {
    private final EventRepository eventRepository;
    private final EventTaskRepository taskRepository;

    public EventService(EventRepository eventRepository, EventTaskRepository taskRepository) {
        this.eventRepository = eventRepository;
        this.taskRepository = taskRepository;
    }

    public List<EventDetails> organizerEvents(long organizerId) {
        return eventRepository.findByOrganizer(organizerId);
    }

    public Event createEvent(String title,
                             String description,
                             LocalDate startDate,
                             LocalDate endDate,
                             long cityId,
                             long directionId,
                             long organizerId,
                             int capacity) {
        return eventRepository.create(title, description, startDate, endDate, cityId, directionId, organizerId, capacity);
    }

    public List<EventDetails> catalog() {
        return eventRepository.findUpcomingCatalog();
    }

    public List<EventDetails> allEvents() {
        return eventRepository.findAll();
    }

    public Optional<Event> findEvent(long id) {
        return eventRepository.findById(id);
    }

    public void updateStatus(long eventId, String status) {
        eventRepository.updateStatus(eventId, status);
    }

    public EventTask addTask(long eventId, String stage, String title, TaskStatus status, LocalDate dueDate, String assignee, String notes) {
        return taskRepository.create(eventId, stage, title, status, dueDate, assignee, notes);
    }

    public List<EventTask> tasksForEvent(long eventId) {
        return taskRepository.findByEvent(eventId);
    }

    public void moveTask(long taskId, TaskStatus status) {
        taskRepository.updateStatus(taskId, status);
    }
}
