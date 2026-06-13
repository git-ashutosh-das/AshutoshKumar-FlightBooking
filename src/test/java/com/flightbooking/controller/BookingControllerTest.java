package com.flightbooking.controller;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flightbooking.dto.BookingRequest;
import com.flightbooking.model.Flight;
import com.flightbooking.model.SeatStatus;
import com.flightbooking.repository.FlightRepository;
import com.flightbooking.service.BookingService;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private BookingService bookingService;

    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        idempotencyKey = UUID.randomUUID().toString();
        resetSeats("flight1", List.of("1", "2", "4"));
    }

    private void resetSeats(String flightNumber, List<String> seatNumbers) {
        Flight flight = flightRepository.findByFlightNumber(flightNumber).orElseThrow();
        for (String seatNumber : seatNumbers) {
            flight.findSeat(seatNumber).orElseThrow().setStatus(SeatStatus.AVAILABLE);
        }
    }

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("successful single-seat booking returns 201 with booking details")
        void successfulBooking_singleSeat() throws Exception {
            mockMvc.perform(post("/api/bookings")
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "flight": "flight1",
                                      "seats": ["1"],
                                      "name": "Ashutosh Kumar"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.flight").value("flight1"))
                    .andExpect(jsonPath("$.seats[0]").value("1"))
                    .andExpect(jsonPath("$.name").value("Ashutosh Kumar"));
        }

        @Test
        @DisplayName("successful multi-seat booking returns 201 with all seats")
        void successfulBooking_multipleSeats() throws Exception {
            mockMvc.perform(post("/api/bookings")
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "flight": "flight1",
                                      "seats": ["1", "2"],
                                      "name": "Ashutosh Kumar"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.flight").value("flight1"))
                    .andExpect(jsonPath("$.seats.length()").value(2))
                    .andExpect(jsonPath("$.seats[0]").value("1"))
                    .andExpect(jsonPath("$.seats[1]").value("2"))
                    .andExpect(jsonPath("$.name").value("Ashutosh Kumar"));
        }
    }

    @Nested
    @DisplayName("Invalid flight")
    class InvalidFlight {

        @Test
        @DisplayName("unknown flight number returns 404")
        void unknownFlight_returnsNotFound() throws Exception {
            mockMvc.perform(post("/api/bookings")
                            .header("Idempotency-Key", idempotencyKey)
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
    }

    @Nested
    @DisplayName("Invalid seat")
    class InvalidSeat {

        @Test
        @DisplayName("seat not on flight returns 404")
        void seatNotOnFlight_returnsNotFound() throws Exception {
            mockMvc.perform(post("/api/bookings")
                            .header("Idempotency-Key", idempotencyKey)
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
        @DisplayName("multi-seat request with one invalid seat fails entire request")
        void oneInvalidSeatInMultiSeatRequest_failsEntireRequest() throws Exception {
            mockMvc.perform(post("/api/bookings")
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "flight": "flight1",
                                      "seats": ["1", "99"],
                                      "name": "Ashutosh Kumar"
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Seat 99 not found on flight flight1"));

            Flight flight = flightRepository.findByFlightNumber("flight1").orElseThrow();
            assertThat(flight.findSeat("1").orElseThrow().getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        }
    }

    @Nested
    @DisplayName("Overflow booking")
    class OverflowBooking {

        @Test
        @DisplayName("booking an already-booked seat returns 409")
        void alreadyBookedSeat_returnsConflict() throws Exception {
            BookingRequest firstBooking = new BookingRequest();
            firstBooking.setFlight("flight1");
            firstBooking.setSeats(List.of("1"));
            firstBooking.setName("First Passenger");
            bookingService.createBooking("overflow-first", firstBooking);

            mockMvc.perform(post("/api/bookings")
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "flight": "flight1",
                                      "seats": ["1"],
                                      "name": "Second Passenger"
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("Seat 1 on flight flight1 is already booked"));
        }

        @Test
        @DisplayName("multi-seat request with one already-booked seat fails entire request")
        void oneAlreadyBookedSeatInMultiSeatRequest_failsEntireRequest() throws Exception {
            BookingRequest firstBooking = new BookingRequest();
            firstBooking.setFlight("flight1");
            firstBooking.setSeats(List.of("2"));
            firstBooking.setName("First Passenger");
            bookingService.createBooking("overflow-partial-first", firstBooking);

            mockMvc.perform(post("/api/bookings")
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "flight": "flight1",
                                      "seats": ["1", "2"],
                                      "name": "Second Passenger"
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("Seat 2 on flight flight1 is already booked"));

            Flight flight = flightRepository.findByFlightNumber("flight1").orElseThrow();
            assertThat(flight.findSeat("1").orElseThrow().getStatus()).isEqualTo(SeatStatus.AVAILABLE);
            assertThat(flight.findSeat("2").orElseThrow().getStatus()).isEqualTo(SeatStatus.BOOKED);
        }
    }
}
