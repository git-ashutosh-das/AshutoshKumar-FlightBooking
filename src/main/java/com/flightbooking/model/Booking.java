package com.flightbooking.model;

import java.util.List;
import java.util.UUID;

public class Booking {

    private final String id;
    private final String flightNumber;
    private final List<String> seatNumbers;
    private final String passengerName;

    public Booking(String flightNumber, List<String> seatNumbers, String passengerName) {
        this.id = UUID.randomUUID().toString();
        this.flightNumber = flightNumber;
        this.seatNumbers = List.copyOf(seatNumbers);
        this.passengerName = passengerName;
    }

    public String getId() {
        return id;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public List<String> getSeatNumbers() {
        return seatNumbers;
    }

    public String getPassengerName() {
        return passengerName;
    }
}
