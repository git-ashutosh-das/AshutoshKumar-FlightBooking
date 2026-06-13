package com.flightbooking.exception;

public class SeatNotAvailableException extends RuntimeException {

    public SeatNotAvailableException(String flightNumber, String seatNumber) {
        super("Seat " + seatNumber + " on flight " + flightNumber + " is already booked");
    }
}
