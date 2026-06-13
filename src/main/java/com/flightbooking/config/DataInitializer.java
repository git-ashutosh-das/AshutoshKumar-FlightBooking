package com.flightbooking.config;

import com.flightbooking.model.Flight;
import com.flightbooking.repository.FlightRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

/** Seeds the in-memory flight catalog when the application starts. */
@Component
public class DataInitializer implements CommandLineRunner {

    private final FlightRepository flightRepository;

    /**
     * @param flightRepository repository used to load sample flights at startup
     */
    public DataInitializer(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    /**
     * @param args command-line arguments passed to the application (unused)
     */
    @Override
    public void run(String... args) {
        flightRepository.save(new Flight("flight1", seatNumbers(6)));
        flightRepository.save(new Flight("flight2", seatNumbers(8)));
        flightRepository.save(new Flight("flight3", seatNumbers(10)));
    }

    /** Builds seat labels "1" through {@code count} inclusive. */
    private static List<String> seatNumbers(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(String::valueOf)
                .toList();
    }
}
