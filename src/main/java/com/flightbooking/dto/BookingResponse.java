package com.flightbooking.dto;

import java.util.List;

/** API response body returned after a successful booking (new or idempotent replay). */
public class BookingResponse {

    private final String id;
    private final String flight;
    private final List<String> seats;
    private final String name;

    /**
     * @param id     unique booking identifier assigned at creation time
     * @param flight flight number that was booked
     * @param seats  seat labels confirmed in this booking
     * @param name   passenger name on the booking
     */
    public BookingResponse(String id, String flight, List<String> seats, String name) {
        this.id = id;
        this.flight = flight;
        this.seats = seats;
        this.name = name;
    }

    /** @return unique booking id */
    public String getId() {
        return id;
    }

    /** @return booked flight number */
    public String getFlight() {
        return flight;
    }

    /** @return confirmed seat labels */
    public List<String> getSeats() {
        return seats;
    }

    /** @return passenger name */
    public String getName() {
        return name;
    }
}
