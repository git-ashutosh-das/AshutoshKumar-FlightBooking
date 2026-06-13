package com.flightbooking.model;

import java.util.UUID;

public class Booking {

    private final String id;
    private final String flightNumber;
    private final String seatNumber;
    private final String passengerName;

    public Booking(String flightNumber, String seatNumber, String passengerName) {
        this.id = UUID.randomUUID().toString();
        this.flightNumber = flightNumber;
        this.seatNumber = seatNumber;
        this.passengerName = passengerName;
    }

    public String getId() {
        return id;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public String getPassengerName() {
        return passengerName;
    }
}
