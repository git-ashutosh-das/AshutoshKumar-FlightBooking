package com.flightbooking.repository;

import com.flightbooking.model.Flight;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory flight catalog. Flights (and their seats) are loaded at startup and shared
 * across all booking threads for the lifetime of the application.
 */
@Repository
public class FlightRepository {

    private final Map<String, Flight> flights = new ConcurrentHashMap<>();

    /**
     * @param flight flight to add or replace in the catalog
     */
    public void save(Flight flight) {
        flights.put(flight.getFlightNumber(), flight);
    }

    /**
     * @param flightNumber airline flight identifier to look up (e.g. "flight1")
     * @return the flight if known, otherwise empty
     */
    public Optional<Flight> findByFlightNumber(String flightNumber) {
        return Optional.ofNullable(flights.get(flightNumber));
    }
}
