package com.flightbooking.service;

import com.flightbooking.dto.BookingRequest;
import com.flightbooking.exception.SeatNotAvailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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

    @Test
    void createBooking_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flight": "AI101",
                                  "seat": "1A",
                                  "name": "Ashutosh Kumar"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flight").value("AI101"))
                .andExpect(jsonPath("$.seat").value("1A"))
                .andExpect(jsonPath("$.name").value("Ashutosh Kumar"));
    }

    @Test
    void createBooking_flightNotFound_returnsNotFound() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flight": "XX999",
                                  "seat": "1A",
                                  "name": "Ashutosh Kumar"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Flight not found: XX999"));
    }

    @Test
    void createBooking_seatNotFound_returnsNotFound() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flight": "AI101",
                                  "seat": "9Z",
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
        request.setSeat("2B");
        request.setName("First Passenger");
        bookingService.createBooking(request);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flight": "AI202",
                                  "seat": "2B",
                                  "name": "Second Passenger"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Seat 2B on flight AI202 is already booked"));
    }

    @Test
    void createBooking_missingFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flight": "AI101",
                                  "seat": "1B"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Passenger name is required"));
    }
}
