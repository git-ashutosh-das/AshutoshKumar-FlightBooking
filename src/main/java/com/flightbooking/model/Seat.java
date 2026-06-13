package com.flightbooking.model;

import java.util.concurrent.locks.ReentrantLock;

public class Seat {

    private final String seatNumber;
    private SeatStatus status;

    // Per-seat lock enables fine-grained concurrency: requests for non-overlapping seats
    // on the same flight can proceed in parallel instead of serializing on a flight lock.
    private final ReentrantLock lock = new ReentrantLock();

    public Seat(String seatNumber) {
        this.seatNumber = seatNumber;
        this.status = SeatStatus.AVAILABLE;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    public ReentrantLock getLock() {
        return lock;
    }
}
