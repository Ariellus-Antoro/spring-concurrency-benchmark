package com.ttu.platform_threads.dto;

public record CheckoutResponse(
        Long ticketId,
        String status,
        String token
) {

}
