package com.example.eventapp.service;

import com.example.eventapp.domain.City;
import com.example.eventapp.domain.Direction;
import com.example.eventapp.repository.CityRepository;
import com.example.eventapp.repository.DirectionRepository;

import java.util.List;

public class ReferenceDataService {
    private final CityRepository cityRepository;
    private final DirectionRepository directionRepository;

    public ReferenceDataService(CityRepository cityRepository, DirectionRepository directionRepository) {
        this.cityRepository = cityRepository;
        this.directionRepository = directionRepository;
    }

    public List<City> cities() {
        return cityRepository.findAll();
    }

    public List<Direction> directions() {
        return directionRepository.findAll();
    }
}
