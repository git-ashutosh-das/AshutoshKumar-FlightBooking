package com.flightbooking.service;

import com.flightbooking.dto.BookingRequest;
import com.flightbooking.dto.BookingResponse;
import com.flightbooking.exception.FlightNotFoundException;
import com.flightbooking.exception.SeatNotAvailableException;
import com.flightbooking.exception.SeatNotFoundException;
import com.flightbooking.model.Booking;
import com.flightbooking.model.Flight;
import com.flightbooking.model.Seat;
import com.flightbooking.model.SeatStatus;
import com.flightbooking.repository.BookingRepository;
import com.flightbooking.repository.FlightRepository;
import org.springframework.stereotype.Service;

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

        synchronized (flight) {
            Seat seat = flight.findSeat(request.getSeat())
                    .orElseThrow(() -> new SeatNotFoundException(request.getFlight(), request.getSeat()));

            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new SeatNotAvailableException(request.getFlight(), request.getSeat());
            }

            seat.setStatus(SeatStatus.BOOKED);
            Booking booking = bookingRepository.save(
                    new Booking(request.getFlight(), request.getSeat(), request.getName()));

            return new BookingResponse(
                    booking.getId(),
                    booking.getFlightNumber(),
                    booking.getSeatNumber(),
                    booking.getPassengerName());
        }
    }
}
