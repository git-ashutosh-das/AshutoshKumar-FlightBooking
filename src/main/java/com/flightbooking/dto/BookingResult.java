package com.flightbooking.dto;

public class BookingResult {

    private final BookingResponse response;
    private final boolean replayed;

    public BookingResult(BookingResponse response, boolean replayed) {
        this.response = response;
        this.replayed = replayed;
    }

    public BookingResponse getResponse() {
        return response;
    }

    public boolean isReplayed() {
        return replayed;
    }
}
