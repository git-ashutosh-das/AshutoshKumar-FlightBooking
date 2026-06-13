package com.flightbooking.dto;

/**
 * Internal wrapper returned by the service so the controller can choose HTTP status.
 * {@code replayed=true} means the response came from the idempotency cache, not a fresh booking.
 */
public class BookingResult {

    private final BookingResponse response;
    private final boolean replayed;

    /**
     * @param response booking details to return to the client
     * @param replayed {@code true} if {@code response} was loaded from the idempotency cache
     */
    public BookingResult(BookingResponse response, boolean replayed) {
        this.response = response;
        this.replayed = replayed;
    }

    /** @return the booking response payload */
    public BookingResponse getResponse() {
        return response;
    }

    /**
     * @return {@code true} when this result was served from a prior successful idempotent request
     */
    public boolean isReplayed() {
        return replayed;
    }
}
