package com.flightbooking.exception;

/** Thrown when a seat label is not configured on the given flight (HTTP 404). */
public class SeatNotFoundException extends RuntimeException {

    /**
     * @param flightNumber flight that was being booked
     * @param seatNumber   seat label that does not exist on that flight
     */
    public SeatNotFoundException(String flightNumber, String seatNumber) {
        super("Seat " + seatNumber + " not found on flight " + flightNumber);
    }
}
