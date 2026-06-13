package com.flightbooking.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A flight with a fixed set of seats. Seat objects are shared mutable state used during booking;
 * they hold both availability status and per-seat locks for concurrency control.
 */
public class Flight {

    private final String flightNumber;
    private final List<Seat> seats;
    private final Map<String, Seat> seatsByNumber = new HashMap<>();

    /**
     * @param flightNumber airline flight identifier (e.g. "flight1")
     * @param seatNumbers  labels of all seats on this flight (e.g. "1", "2", "3")
     */
    public Flight(String flightNumber, List<String> seatNumbers) {
        this.flightNumber = flightNumber;
        this.seats = new ArrayList<>();
        for (String seatNumber : seatNumbers) {
            Seat seat = new Seat(seatNumber);
            this.seats.add(seat);
            this.seatsByNumber.put(seatNumber, seat);
        }
    }

    /** @return flight number (e.g. "flight1") */
    public String getFlightNumber() {
        return flightNumber;
    }

    /**
     * @return unmodifiable list of all seats on this flight
     */
    public List<Seat> getSeats() {
        return Collections.unmodifiableList(seats);
    }

    /**
     * @param seatNumber seat label to look up (e.g. "1")
     * @return the matching seat, or empty if this flight has no such seat
     */
    public Optional<Seat> findSeat(String seatNumber) {
        return Optional.ofNullable(seatsByNumber.get(seatNumber));
    }
}
