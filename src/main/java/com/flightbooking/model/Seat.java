package com.flightbooking.model;

import java.util.concurrent.locks.ReentrantLock;

/**
 * A single seat on a flight. Combines availability state with a dedicated lock so
 * {@link com.flightbooking.service.BookingService} can coordinate concurrent multi-seat requests.
 */
public class Seat {

    private final String seatNumber;
    private SeatStatus status;

    // Per-seat lock enables fine-grained concurrency: requests for non-overlapping seats
    // on the same flight can proceed in parallel instead of serializing on a flight-wide lock.
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * @param seatNumber seat label (e.g. "1"); starts as {@link SeatStatus#AVAILABLE}
     */
    public Seat(String seatNumber) {
        this.seatNumber = seatNumber;
        this.status = SeatStatus.AVAILABLE;
    }

    /** @return seat label (e.g. "1") */
    public String getSeatNumber() {
        return seatNumber;
    }

    /** @return current availability state of this seat */
    public SeatStatus getStatus() {
        return status;
    }

    /**
     * @param status new availability state; set to {@link SeatStatus#BOOKED} on successful reservation
     */
    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    /** Lock used during the acquire/validate/commit phases of a booking attempt.
     *
     * @return per-seat reentrant lock for coordinating concurrent bookings
     */
    public ReentrantLock getLock() {
        return lock;
    }
}
