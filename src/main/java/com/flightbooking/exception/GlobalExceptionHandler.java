package com.flightbooking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps domain and validation exceptions to consistent JSON error responses.
 *
 * Status mapping:
 * - 404: flight or seat does not exist
 * - 409: seat contention (already booked or lock not acquired under concurrent load)
 * - 400: request validation failures from {@link com.flightbooking.dto.BookingRequest}
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * @param ex exception raised when the flight number is not in the catalog
     * @return HTTP 404 with {@code {"error": "<message>"}}
     */
    @ExceptionHandler(FlightNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleFlightNotFound(FlightNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(ex.getMessage()));
    }

    /**
     * @param ex exception raised when a seat label is not on the requested flight
     * @return HTTP 404 with {@code {"error": "<message>"}}
     */
    @ExceptionHandler(SeatNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleSeatNotFound(SeatNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(ex.getMessage()));
    }

    /**
     * @param ex exception raised when a seat is already booked at validation time
     * @return HTTP 409 with {@code {"error": "<message>"}}
     */
    @ExceptionHandler(SeatNotAvailableException.class)
    public ResponseEntity<Map<String, String>> handleSeatNotAvailable(SeatNotAvailableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody(ex.getMessage()));
    }

    /**
     * @param ex exception raised when a seat lock cannot be acquired under concurrent load
     * @return HTTP 409 with {@code {"error": "<message>"}}
     */
    @ExceptionHandler(SeatLockNotAcquiredException.class)
    public ResponseEntity<Map<String, String>> handleSeatLockNotAcquired(SeatLockNotAcquiredException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody(ex.getMessage()));
    }

    /**
     * @param ex Spring validation exception from {@link jakarta.validation.Valid} on the request body
     * @return HTTP 400 with the first field or global validation message
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElseGet(() -> ex.getBindingResult().getGlobalErrors().stream()
                        .findFirst()
                        .map(error -> error.getDefaultMessage())
                        .orElse("Invalid request"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(message));
    }

    /**
     * @param message human-readable error description
     * @return JSON body in the form {@code {"error": "<message>"}}
     */
    private Map<String, String> errorBody(String message) {
        Map<String, String> body = new HashMap<>();
        body.put("error", message);
        return body;
    }
}
