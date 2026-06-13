package com.flightbooking.repository;

import com.flightbooking.dto.BookingResponse;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class IdempotencyRepository {

    private final Map<String, BookingResponse> responses = new ConcurrentHashMap<>();

    public Optional<BookingResponse> findByKey(String idempotencyKey) {
        return Optional.ofNullable(responses.get(idempotencyKey));
    }

    public void save(String idempotencyKey, BookingResponse response) {
        responses.put(idempotencyKey, response);
    }
}
