package com.flightbooking.dto;

import java.util.HashSet;
import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public class BookingRequest {

    @NotBlank(message = "Flight number is required")
    private String flight;

    @NotEmpty(message = "At least one seat is required")
    @Size(max = 4, message = "A booking can include at most 4 seats")
    private List<@NotBlank(message = "Seat number cannot be blank") String> seats;

    @NotBlank(message = "Passenger name is required")
    private String name;

    @AssertTrue(message = "Duplicate seat numbers are not allowed in a single booking")
    public boolean isSeatsUnique() {
        return seats == null || seats.size() == new HashSet<>(seats).size();
    }

    public String getFlight() {
        return flight;
    }

    public void setFlight(String flight) {
        this.flight = flight;
    }

    public List<String> getSeats() {
        return seats;
    }

    public void setSeats(List<String> seats) {
        this.seats = seats;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
