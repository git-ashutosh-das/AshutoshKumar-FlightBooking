package com.flightbooking.dto;

import java.util.HashSet;
import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * Incoming booking payload. Bean Validation runs in the controller before service logic;
 * invalid requests never reach seat locking or idempotency handling.
 */
public class BookingRequest {

    /** Airline flight number to book seats on (e.g. "flight1"). */
    @NotBlank(message = "Flight number is required")
    private String flight;

    /** Seat labels to reserve; must contain 1–4 unique, non-blank entries (e.g. ["1", "2"]). */
    @NotEmpty(message = "At least one seat is required")
    @Size(max = 4, message = "A booking can include at most 4 seats")
    private List<@NotBlank(message = "Seat number cannot be blank") String> seats;

    /** Full name of the passenger making the booking. */
    @NotBlank(message = "Passenger name is required")
    private String name;

    /**
     * Rejects duplicate seat numbers in a single request (e.g. ["1", "1"]).
     *
     * @return {@code true} when all seat entries are distinct
     */
    @AssertTrue(message = "Duplicate seat numbers are not allowed in a single booking")
    public boolean isSeatsUnique() {
        return seats == null || seats.size() == new HashSet<>(seats).size();
    }

    /** @return flight number being booked */
    public String getFlight() {
        return flight;
    }

    /** @param flight airline flight number (e.g. "flight1") */
    public void setFlight(String flight) {
        this.flight = flight;
    }

    /** @return seat labels requested for this booking */
    public List<String> getSeats() {
        return seats;
    }

    /** @param seats seat labels to reserve (1–4 unique entries) */
    public void setSeats(List<String> seats) {
        this.seats = seats;
    }

    /** @return passenger name */
    public String getName() {
        return name;
    }

    /** @param name full name of the passenger */
    public void setName(String name) {
        this.name = name;
    }
}
