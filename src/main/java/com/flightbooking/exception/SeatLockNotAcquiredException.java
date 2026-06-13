package com.flightbooking.exception;

public class SeatLockNotAcquiredException extends RuntimeException {

    public SeatLockNotAcquiredException(String flightNumber) {
        super("Could not acquire lock for all requested seats on flight " + flightNumber);
    }
}
