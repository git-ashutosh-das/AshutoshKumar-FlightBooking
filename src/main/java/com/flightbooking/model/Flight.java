package com.flightbooking.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Flight {

    private final String flightNumber;
    private final List<Seat> seats;

    public Flight(String flightNumber, List<String> seatNumbers) {
        this.flightNumber = flightNumber;
        this.seats = new ArrayList<>();
        for (String seatNumber : seatNumbers) {
            this.seats.add(new Seat(seatNumber));
        }
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public List<Seat> getSeats() {
        return Collections.unmodifiableList(seats);
    }

    public Optional<Seat> findSeat(String seatNumber) {
        return seats.stream()
                .filter(seat -> seat.getSeatNumber().equals(seatNumber))
                .findFirst();
    }
}
