package com.flightbooking.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.flightbooking.dto.BookingRequest;
import com.flightbooking.dto.BookingResponse;
import com.flightbooking.dto.BookingResult;
import com.flightbooking.exception.FlightNotFoundException;
import com.flightbooking.exception.SeatLockNotAcquiredException;
import com.flightbooking.exception.SeatNotAvailableException;
import com.flightbooking.exception.SeatNotFoundException;
import com.flightbooking.model.Booking;
import com.flightbooking.model.Flight;
import com.flightbooking.model.Seat;
import com.flightbooking.model.SeatStatus;
import com.flightbooking.repository.BookingRepository;
import com.flightbooking.repository.FlightRepository;
import com.flightbooking.repository.IdempotencyRepository;

/**
 * Core booking logic: idempotent request handling plus concurrent, multi-seat seat reservation.
 *
 * Concurrency model:
 * - Per-seat {@link com.flightbooking.model.Seat} locks allow parallel bookings for non-overlapping seats.
 * - Seats are locked in a consistent sorted order to avoid deadlock.
 * - {@code tryLock} makes overlapping requests fail fast (409) instead of blocking.
 *
 * Idempotency model:
 * - Clients send an {@code Idempotency-Key} header; successful responses are cached and replayed on retry.
 * - Per-key synchronization prevents duplicate bookings when the same key arrives concurrently.
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final FlightRepository flightRepository;
    private final BookingRepository bookingRepository;
    private final IdempotencyRepository idempotencyRepository;

    // One monitor object per idempotency key so concurrent retries serialize without blocking other keys.
    private final ConcurrentHashMap<String, Object> idempotencyLocks = new ConcurrentHashMap<>();

    /**
     * @param flightRepository     source of flight and seat data
     * @param bookingRepository    store for completed bookings
     * @param idempotencyRepository cache of successful responses keyed by idempotency key
     */
    public BookingService(FlightRepository flightRepository,
                          BookingRepository bookingRepository,
                          IdempotencyRepository idempotencyRepository) {
        this.flightRepository = flightRepository;
        this.bookingRepository = bookingRepository;
        this.idempotencyRepository = idempotencyRepository;
    }

    /**
     * Entry point for creating a booking. Returns the cached response when the same
     * idempotency key was already used successfully; otherwise executes a new booking.
     *
     * @param idempotencyKey unique client-provided key used to deduplicate retries
     * @param request        validated booking details (flight, seats, passenger name)
     * @return booking response plus a flag indicating whether it was replayed from cache
     */
    public BookingResult createBooking(String idempotencyKey, BookingRequest request) {
        Object lock = idempotencyLocks.computeIfAbsent(idempotencyKey, k -> new Object());
        synchronized (lock) {
            return idempotencyRepository.findByKey(idempotencyKey)
                    .map(response -> {
                        log.debug("Idempotent replay: key={}, bookingId={}", idempotencyKey, response.getId());
                        return new BookingResult(response, true);
                    })
                    .orElseGet(() -> {
                        BookingResponse response = executeBooking(request);
                        idempotencyRepository.save(idempotencyKey, response);
                        return new BookingResult(response, false);
                    });
        }
    }

    /**
     * Reserves seats using a three-phase protocol: acquire locks, validate availability, commit.
     * Any failure before commit rolls back by releasing held locks and leaving seat state unchanged.
     *
     * @param request booking details for the flight and seats to reserve
     * @return response containing the new booking id and confirmed seat list
     * @throws FlightNotFoundException        if the flight number is unknown
     * @throws SeatNotFoundException          if any seat is not on the flight
     * @throws SeatLockNotAcquiredException   if a concurrent request holds a required seat lock
     * @throws SeatNotAvailableException      if any requested seat is already booked
     */
    private BookingResponse executeBooking(BookingRequest request) {
        Flight flight = flightRepository.findByFlightNumber(request.getFlight())
                .orElseThrow(() -> new FlightNotFoundException(request.getFlight()));

        // Sort seat numbers lexicographically so every thread acquires locks in the
        // same global order, preventing deadlock when two multi-seat requests overlap.
        List<String> sortedSeatNumbers = request.getSeats().stream().sorted().toList();

        // Resolve seats before locking (404 if any seat does not exist on this flight).
        List<Seat> seats = new ArrayList<>();
        for (String seatNumber : sortedSeatNumbers) {
            seats.add(flight.findSeat(seatNumber)
                    .orElseThrow(() -> new SeatNotFoundException(request.getFlight(), seatNumber)));
        }

        List<Seat> lockedSeats = new ArrayList<>();
        try {
            // Phase 1 — Acquire: use tryLock (non-blocking) so contending requests fail
            // fast with 409 instead of waiting for the other request to finish.
            for (Seat seat : seats) {
                if (!seat.getLock().tryLock()) {
                    log.debug("Seat lock unavailable: flight={}, seat={}", request.getFlight(), seat.getSeatNumber());
                    throw new SeatLockNotAcquiredException(request.getFlight());
                }
                lockedSeats.add(seat);
            }

            // Phase 2 — Validate: check availability while holding all locks; no mutations yet.
            for (Seat seat : seats) {
                if (seat.getStatus() != SeatStatus.AVAILABLE) {
                    log.debug("Seat already booked: flight={}, seat={}", request.getFlight(), seat.getSeatNumber());
                    throw new SeatNotAvailableException(request.getFlight(), seat.getSeatNumber());
                }
            }

            // Phase 3 — Commit: all seats passed validation, so book every seat atomically.
            for (Seat seat : seats) {
                seat.setStatus(SeatStatus.BOOKED);
            }
            Booking booking = bookingRepository.save(
                    new Booking(request.getFlight(), request.getSeats(), request.getName()));

            log.debug("Booking created: id={}, flight={}, seats={}",
                    booking.getId(), booking.getFlightNumber(), booking.getSeatNumbers());

            return new BookingResponse(
                    booking.getId(),
                    booking.getFlightNumber(),
                    booking.getSeatNumbers(),
                    booking.getPassengerName());
        } finally {
            releaseLocks(lockedSeats);
        }
    }

    /**
     * Releases seat locks acquired during a booking attempt.
     *
     * @param lockedSeats seats whose locks were acquired, in acquisition order
     */
    private void releaseLocks(List<Seat> lockedSeats) {
        for (int i = lockedSeats.size() - 1; i >= 0; i--) {
            lockedSeats.get(i).getLock().unlock();
        }
    }
}
