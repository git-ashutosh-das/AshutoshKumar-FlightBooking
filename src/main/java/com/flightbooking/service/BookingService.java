package com.flightbooking.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.flightbooking.dto.BookingRequest;
import com.flightbooking.dto.BookingResponse;
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

@Service
public class BookingService {

    private final FlightRepository flightRepository;
    private final BookingRepository bookingRepository;

    public BookingService(FlightRepository flightRepository, BookingRepository bookingRepository) {
        this.flightRepository = flightRepository;
        this.bookingRepository = bookingRepository;
    }

    public BookingResponse createBooking(BookingRequest request) {
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
                    throw new SeatLockNotAcquiredException(request.getFlight());
                }
                lockedSeats.add(seat);
            }

            // Phase 2 — Validate: check availability while holding all locks; no mutations yet.
            for (Seat seat : seats) {
                if (seat.getStatus() != SeatStatus.AVAILABLE) {
                    throw new SeatNotAvailableException(request.getFlight(), seat.getSeatNumber());
                }
            }

            // Phase 3 — Commit: all seats passed validation, so book every seat atomically.
            for (Seat seat : seats) {
                seat.setStatus(SeatStatus.BOOKED);
            }
            Booking booking = bookingRepository.save(
                    new Booking(request.getFlight(), request.getSeats(), request.getName()));

            return new BookingResponse(
                    booking.getId(),
                    booking.getFlightNumber(),
                    booking.getSeatNumbers(),
                    booking.getPassengerName());
        } finally {
            releaseLocks(lockedSeats);
        }
    }

    // Release in reverse acquisition order; safe convention when backing out of partial acquire.
    private void releaseLocks(List<Seat> lockedSeats) {
        for (int i = lockedSeats.size() - 1; i >= 0; i--) {
            lockedSeats.get(i).getLock().unlock();
        }
    }
}
