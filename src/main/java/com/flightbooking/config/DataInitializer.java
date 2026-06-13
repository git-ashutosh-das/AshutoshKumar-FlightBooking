package com.flightbooking.config;

import com.flightbooking.model.Flight;
import com.flightbooking.repository.FlightRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final FlightRepository flightRepository;

    public DataInitializer(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    @Override
    public void run(String... args) {
        flightRepository.save(new Flight("AI101", List.of("1A", "1B", "1C", "2A", "2B", "2C")));
        flightRepository.save(new Flight("AI202", List.of("1A", "1B", "2A", "2B")));
        flightRepository.save(new Flight("6E303", List.of("1A", "1B", "1C", "1D")));
    }
}
