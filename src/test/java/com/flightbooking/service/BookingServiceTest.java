package com.flightbooking.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.flightbooking.dto.BookingRequest;
import com.flightbooking.dto.BookingResult;
import com.flightbooking.exception.SeatLockNotAcquiredException;
import com.flightbooking.exception.SeatNotAvailableException;
import com.flightbooking.model.Flight;
import com.flightbooking.model.SeatStatus;
import com.flightbooking.repository.FlightRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private FlightRepository flightRepository;

    private void resetSeats(String flightNumber, List<String> seatNumbers) {
        Flight flight = flightRepository.findByFlightNumber(flightNumber).orElseThrow();
        for (String seatNumber : seatNumbers) {
            flight.findSeat(seatNumber).orElseThrow().setStatus(SeatStatus.AVAILABLE);
        }
    }

    private String uniqueKey() {
        return UUID.randomUUID().toString();
    }

    @Test
    void createBooking_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Idempotency-Key", uniqueKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flight": "flight1",
                                  "seats": ["1"],
                                  "name": "Ashutosh Kumar"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flight").value("flight1"))
                .andExpect(jsonPath("$.seats[0]").value("1"))
                .andExpect(jsonPath("$.name").value("Ashutosh Kumar"));
    }

    @Test
    void createBooking_multipleSeats_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Idempotency-Key", uniqueKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flight": "flight2",
                                  "seats": ["1", "2", "3"],
                                  "name": "Ashutosh Kumar"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seats.length()").value(3))
                .andExpect(jsonPath("$.seats[0]").value("1"))
                .andExpect(jsonPath("$.seats[1]").value("2"))
                .andExpect(jsonPath("$.seats[2]").value("3"));
    }

    @Test
    void createBooking_flightNotFound_returnsNotFound() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Idempotency-Key", uniqueKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flight": "XX999",
                                  "seats": ["1"],
                                  "name": "Ashutosh Kumar"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Flight not found: XX999"));
    }

    @Test
    void createBooking_seatNotFound_returnsNotFound() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Idempotency-Key", uniqueKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flight": "flight1",
                                  "seats": ["99"],
                                  "name": "Ashutosh Kumar"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Seat 99 not found on flight flight1"));
    }

    @Test
    void createBooking_seatAlreadyBooked_returnsConflict() throws Exception {
        BookingRequest request = new BookingRequest();
        request.setFlight("flight2");
        request.setSeats(List.of("5"));
        request.setName("First Passenger");
        bookingService.createBooking("seat-already-booked-first", request);

        mockMvc.perform(post("/api/bookings")
                        .header("Idempotency-Key", uniqueKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flight": "flight2",
                                  "seats": ["5"],
                                  "name": "Second Passenger"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Seat 5 on flight flight2 is already booked"));
    }

    @Test
    void createBooking_oneSeatUnavailable_failsEntireRequest() {
        resetSeats("flight1", List.of("1", "2"));

        BookingRequest first = new BookingRequest();
        first.setFlight("flight1");
        first.setSeats(List.of("2"));
        first.setName("First Passenger");
        bookingService.createBooking("partial-booking-first", first);

        BookingRequest second = new BookingRequest();
        second.setFlight("flight1");
        second.setSeats(List.of("1", "2"));
        second.setName("Second Passenger");

        assertThrows(SeatNotAvailableException.class,
                () -> bookingService.createBooking("partial-booking-second", second));

        Flight flight = flightRepository.findByFlightNumber("flight1").orElseThrow();
        assertThat(flight.findSeat("1").orElseThrow().getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(flight.findSeat("2").orElseThrow().getStatus()).isEqualTo(SeatStatus.BOOKED);
    }

    @Test
    void createBooking_concurrentSameIdempotencyKey_createsSingleBooking() throws Exception {
        resetSeats("flight1", List.of("4"));

        String idempotencyKey = "concurrent-idempotency-key";
        BookingRequest request = new BookingRequest();
        request.setFlight("flight1");
        request.setSeats(List.of("4"));
        request.setName("Ashutosh Kumar");

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<BookingResult>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                startLatch.await();
                return bookingService.createBooking(idempotencyKey, request);
            }));
        }

        startLatch.countDown();

        List<BookingResult> results = new ArrayList<>();
        for (Future<BookingResult> future : futures) {
            results.add(future.get());
        }
        executor.shutdown();

        String bookingId = results.get(0).getResponse().getId();
        assertThat(results).extracting(r -> r.getResponse().getId()).containsOnly(bookingId);
        assertThat(results.stream().filter(r -> !r.isReplayed()).count()).isEqualTo(1);
        assertThat(results.stream().filter(BookingResult::isReplayed).count()).isEqualTo(threadCount - 1);

        Flight flight = flightRepository.findByFlightNumber("flight1").orElseThrow();
        assertThat(flight.findSeat("4").orElseThrow().getStatus()).isEqualTo(SeatStatus.BOOKED);
    }

    @Test
    void createBooking_concurrentOverlappingSeats_onlyOneSucceeds() throws Exception {
        resetSeats("flight1", List.of("1", "2", "3", "4", "5"));

        BookingRequest userA = new BookingRequest();
        userA.setFlight("flight1");
        userA.setSeats(List.of("1", "2", "3", "4"));
        userA.setName("User A");

        BookingRequest userB = new BookingRequest();
        userB.setFlight("flight1");
        userB.setSeats(List.of("4", "5"));
        userB.setName("User B");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Future<Object> futureA = executor.submit(() -> {
            startLatch.await();
            try {
                return bookingService.createBooking("overlap-user-a", userA);
            } catch (RuntimeException ex) {
                return ex;
            }
        });
        Future<Object> futureB = executor.submit(() -> {
            startLatch.await();
            try {
                return bookingService.createBooking("overlap-user-b", userB);
            } catch (RuntimeException ex) {
                return ex;
            }
        });

        startLatch.countDown();

        Object resultA = futureA.get();
        Object resultB = futureB.get();
        executor.shutdown();

        assertThat(List.of(resultA, resultB).stream().filter(BookingResult.class::isInstance).count()).isEqualTo(1);
        assertThat(List.of(resultA, resultB).stream().filter(RuntimeException.class::isInstance).count()).isEqualTo(1);
        assertThat(List.of(resultA, resultB).stream().filter(RuntimeException.class::isInstance))
                .allMatch(ex -> ex instanceof SeatLockNotAcquiredException
                        || ex instanceof SeatNotAvailableException);

        Flight flight = flightRepository.findByFlightNumber("flight1").orElseThrow();
        assertThat(flight.findSeat("4").orElseThrow().getStatus()).isEqualTo(SeatStatus.BOOKED);

        if (resultA instanceof BookingResult) {
            assertThat(flight.findSeat("1").orElseThrow().getStatus()).isEqualTo(SeatStatus.BOOKED);
            assertThat(flight.findSeat("2").orElseThrow().getStatus()).isEqualTo(SeatStatus.BOOKED);
            assertThat(flight.findSeat("3").orElseThrow().getStatus()).isEqualTo(SeatStatus.BOOKED);
            assertThat(flight.findSeat("5").orElseThrow().getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        } else {
            assertThat(flight.findSeat("1").orElseThrow().getStatus()).isEqualTo(SeatStatus.AVAILABLE);
            assertThat(flight.findSeat("5").orElseThrow().getStatus()).isEqualTo(SeatStatus.BOOKED);
        }
    }

    @Test
    void createBooking_missingName_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Idempotency-Key", uniqueKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flight": "flight1",
                                  "seats": ["2"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Passenger name is required"));
    }

    @Test
    void createBooking_emptySeatsList_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Idempotency-Key", uniqueKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flight": "flight1",
                                  "seats": [],
                                  "name": "Ashutosh Kumar"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("At least one seat is required"));
    }

    @Test
    void createBooking_moreThanFourSeats_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Idempotency-Key", uniqueKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flight": "flight1",
                                  "seats": ["1", "2", "3", "4", "5"],
                                  "name": "Ashutosh Kumar"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("A booking can include at most 4 seats"));
    }

    @Test
    void createBooking_duplicateSeats_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Idempotency-Key", uniqueKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flight": "flight1",
                                  "seats": ["1", "1"],
                                  "name": "Ashutosh Kumar"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Duplicate seat numbers are not allowed in a single booking"));
    }

    @Test
    void createBooking_blankPassengerName_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Idempotency-Key", uniqueKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flight": "flight1",
                                  "seats": ["1"],
                                  "name": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Passenger name is required"));
    }

    @Test
    void createBooking_missingFlight_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Idempotency-Key", uniqueKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "seats": ["1"],
                                  "name": "Ashutosh Kumar"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Flight number is required"));
    }
}
