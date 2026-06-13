package com.flightbooking.dto;

public class BookingResponse {

    private final String id;
    private final String flight;
    private final String seat;
    private final String name;

    public BookingResponse(String id, String flight, String seat, String name) {
        this.id = id;
        this.flight = flight;
        this.seat = seat;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getFlight() {
        return flight;
    }

    public String getSeat() {
        return seat;
    }

    public String getName() {
        return name;
    }
}
