package com.flightbooking.model;

/** Lifecycle state of a seat on a flight. Only AVAILABLE seats can be booked. */
public enum SeatStatus {
    /** Seat is open for reservation. */
    AVAILABLE,
    /** Seat has been successfully booked and must not be assigned again. */
    BOOKED
}
