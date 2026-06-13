package com.flightbooking.exception;

/** Thrown when a seat exists but is already BOOKED at validation time (HTTP 409). */
public class SeatNotAvailableException extends RuntimeException {

    /**
     * @param flightNumber flight that was being booked
     * @param seatNumber   seat that is no longer available
     */
    public SeatNotAvailableException(String flightNumber, String seatNumber) {
        super("Seat " + seatNumber + " on flight " + flightNumber + " is already booked");
    }
}
