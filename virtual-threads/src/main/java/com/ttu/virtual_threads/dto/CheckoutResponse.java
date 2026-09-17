package com.ttu.virtual_threads.dto;

public record CheckoutResponse(        
    Long ticketId,
    String status,
    String token
) {}
