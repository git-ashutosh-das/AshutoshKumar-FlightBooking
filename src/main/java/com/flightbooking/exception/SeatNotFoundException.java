package com.flightbooking.exception;

public class SeatNotFoundException extends RuntimeException {

    public SeatNotFoundException(String flightNumber, String seatNumber) {
        super("Seat " + seatNumber + " not found on flight " + flightNumber);
    }
}
