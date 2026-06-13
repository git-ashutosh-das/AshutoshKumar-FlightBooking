package com.flightbooking.service;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.flightbooking.dto.BookingRequest;
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
                                  "flight": "AI101",
                                  "seats": ["1A"],
                                  "name": "Ashutosh Kumar"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flight").value("AI101"))
                .andExpect(jsonPath("$.seats[0]").value("1A"))
                .andExpect(jsonPath("$.name").value("Ashutosh Kumar"));
    }

    @Test
    void createBooking_multipleSeats_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Idempotency-Key", uniqueKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flight": "6E303",
                                  "seats": ["1A", "1B", "1C"],
                                  "name": "Ashutosh Kumar"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seats.length()").value(3))
                .andExpect(jsonPath("$.seats[0]").value("1A"))
                .andExpect(jsonPath("$.seats[1]").value("1B"))
                .andExpect(jsonPath("$.seats[2]").value("1C"));
    }

    @Test
    void createBooking_flightNotFound_returnsNotFound() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Idempotency-Key", uniqueKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flight": "XX999",
                                  "seats": ["1A"],
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
                                  "flight": "AI101",
                                  "seats": ["9Z"],
                                  "name": "Ashutosh Kumar"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Seat 9Z not found on flight AI101"));
    }

    @Test
    void createBooking_seatAlreadyBooked_returnsConflict() throws Exception {
        BookingRequest request = new BookingRequest();
        request.setFlight("AI202");
        request.setSeats(List.of("2B"));
        request.setName("First Passenger");
        bookingService.createBooking("seat-already-booked-first", request);

        mockMvc.perform(post("/api/bookings")
                        .header("Idempotency-Key", uniqueKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flight": "AI202",
                                  "seats": ["2B"],
                                  "name": "Second Passenger"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Seat 2B on flight AI202 is already booked"));
    }

    @Test
    void createBooking_oneSeatUnavailable_failsEntireRequest() {
        resetSeats("AI101", List.of("1A", "1B"));

        BookingRequest first = new BookingRequest();
        first.setFlight("AI101");
        first.setSeats(List.of("1B"));
        first.setName("First Passenger");
        bookingService.createBooking("partial-booking-first", first);

        BookingRequest second = new BookingRequest();
        second.setFlight("AI101");
        second.setSeats(List.of("1A", "1B"));
        second.setName("Second Passenger");

        assertThrows(SeatNotAvailableException.class,
                () -> bookingService.createBooking("partial-booking-second", second));

        Flight flight = flightRepository.findByFlightNumber("AI101").orElseThrow();
        assertThat(flight.findSeat("1A").orElseThrow().getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(flight.findSeat("1B").orElseThrow().getStatus()).isEqualTo(SeatStatus.BOOKED);
    }

    @Test
    void createBooking_missingName_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Idempotency-Key", uniqueKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flight": "AI101",
                                  "seats": ["1B"]
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
                                  "flight": "AI101",
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
                                  "flight": "AI101",
                                  "seats": ["1A", "1B", "1C", "2A", "2B"],
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
                                  "flight": "AI101",
                                  "seats": ["1A", "1A"],
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
                                  "flight": "AI101",
                                  "seats": ["1A"],
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
                                  "seats": ["1A"],
                                  "name": "Ashutosh Kumar"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Flight number is required"));
    }
}
