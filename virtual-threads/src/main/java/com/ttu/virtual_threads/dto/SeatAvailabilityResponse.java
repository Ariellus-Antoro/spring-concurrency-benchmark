package com.ttu.virtual_threads.dto;

public record SeatAvailabilityResponse(Long concertId, String concertName, Integer availableSeats) {

}
