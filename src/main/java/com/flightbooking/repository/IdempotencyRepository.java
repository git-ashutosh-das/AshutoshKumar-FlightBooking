package com.flightbooking.repository;

import com.flightbooking.dto.BookingResponse;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores successful booking responses keyed by idempotency key.
 * Only written after a booking completes; failures are not cached so clients can retry.
 * Actual thread safety for check-then-act is enforced in {@link com.flightbooking.service.BookingService}.
 */
@Repository
public class IdempotencyRepository {

    private final Map<String, BookingResponse> responses = new ConcurrentHashMap<>();

    /**
     * @param idempotencyKey client-supplied key from the {@code Idempotency-Key} header
     * @return cached successful response for this key, or empty if none exists yet
     */
    public Optional<BookingResponse> findByKey(String idempotencyKey) {
        return Optional.ofNullable(responses.get(idempotencyKey));
    }

    /**
     * @param idempotencyKey client-supplied key from the {@code Idempotency-Key} header
     * @param response       successful booking response to cache for future replays
     */
    public void save(String idempotencyKey, BookingResponse response) {
        responses.put(idempotencyKey, response);
    }
}
