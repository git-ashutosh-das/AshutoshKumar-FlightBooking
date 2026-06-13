package com.flightbooking.model;

import java.util.List;
import java.util.UUID;

/**
 * Immutable record of a completed booking. Created only after all requested seats
 * have been validated and marked BOOKED.
 */
public class Booking {

    private final String id;
    private final String flightNumber;
    private final List<String> seatNumbers;
    private final String passengerName;

    /**
     * @param flightNumber  flight being booked
     * @param seatNumbers   seats reserved in this booking (1–4 entries)
     * @param passengerName name of the passenger
     */
    public Booking(String flightNumber, List<String> seatNumbers, String passengerName) {
        this.id = UUID.randomUUID().toString();
        this.flightNumber = flightNumber;
        // Defensive copy so later mutations to the request list cannot alter stored booking data.
        this.seatNumbers = List.copyOf(seatNumbers);
        this.passengerName = passengerName;
    }

    /** @return unique booking id assigned at creation */
    public String getId() {
        return id;
    }

    /** @return flight number that was booked */
    public String getFlightNumber() {
        return flightNumber;
    }

    /** @return immutable list of seat labels in this booking */
    public List<String> getSeatNumbers() {
        return seatNumbers;
    }

    /** @return passenger name on the booking */
    public String getPassengerName() {
        return passengerName;
    }
}
