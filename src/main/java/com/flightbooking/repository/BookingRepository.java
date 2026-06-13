package com.flightbooking.repository;

import com.flightbooking.model.Booking;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Persists completed bookings keyed by booking id. Thread-safe for concurrent saves. */
@Repository
public class BookingRepository {

    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();

    /**
     * @param booking completed booking to persist
     * @return the same booking instance that was saved
     */
    public Booking save(Booking booking) {
        bookings.put(booking.getId(), booking);
        return booking;
    }
}
