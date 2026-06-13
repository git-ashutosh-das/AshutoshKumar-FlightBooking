package com.flightbooking.exception;

/** Thrown when the requested flight number is not in the catalog (HTTP 404). */
public class FlightNotFoundException extends RuntimeException {

    /**
     * @param flightNumber unknown flight number supplied by the client
     */
    public FlightNotFoundException(String flightNumber) {
        super("Flight not found: " + flightNumber);
    }
}
