package com.flightbooking.controller;

import com.flightbooking.dto.BookingRequest;
import com.flightbooking.dto.BookingResponse;
import com.flightbooking.service.BookingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.flightbooking.dto.BookingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST entry point for flight seat bookings.
 * Validation runs on {@link BookingRequest} before the service layer is invoked.
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;

    /**
     * @param bookingService service that performs idempotent seat reservation
     */
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Creates a booking for one or more seats on a flight.
     *
     * @param idempotencyKey client-supplied key for safe retries; duplicate successful requests
     *                       return the original response instead of booking again
     * @param request        flight, seat list (1–4 seats), and passenger name
     * @return HTTP 201 with the booking body for a new booking, or HTTP 200 when replaying a cached response
     */
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody BookingRequest request) {
        BookingResult result = bookingService.createBooking(idempotencyKey, request);
        HttpStatus status = result.isReplayed() ? HttpStatus.OK : HttpStatus.CREATED;
        log.debug("POST /api/bookings -> {} bookingId={}", status.value(), result.getResponse().getId());
        return ResponseEntity.status(status).body(result.getResponse());
    }
}
