package com.ttu.reactive.dto;

public record SeatAvailabilityResponse(
    Long concertId, 
    String concertName, 
    Integer availableSeats) {}
