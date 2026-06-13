package com.flightbooking.exception;

/** Thrown when tryLock fails on an overlapping concurrent booking attempt (HTTP 409). */
public class SeatLockNotAcquiredException extends RuntimeException {

    /**
     * @param flightNumber flight on which one or more seat locks could not be acquired
     */
    public SeatLockNotAcquiredException(String flightNumber) {
        super("Could not acquire lock for all requested seats on flight " + flightNumber);
    }
}
