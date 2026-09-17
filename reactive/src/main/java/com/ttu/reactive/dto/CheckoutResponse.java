package com.ttu.reactive.dto;

public record CheckoutResponse(        
    Long ticketId,
    String status,
    String token) {}
