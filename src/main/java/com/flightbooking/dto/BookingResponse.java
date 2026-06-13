package com.flightbooking.dto;

import java.util.List;

public class BookingResponse {

    private final String id;
    private final String flight;
    private final List<String> seats;
    private final String name;

    public BookingResponse(String id, String flight, List<String> seats, String name) {
        this.id = id;
        this.flight = flight;
        this.seats = seats;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getFlight() {
        return flight;
    }

    public List<String> getSeats() {
        return seats;
    }

    public String getName() {
        return name;
    }
}
