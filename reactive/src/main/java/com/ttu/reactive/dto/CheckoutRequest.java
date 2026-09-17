package com.ttu.reactive.dto;

import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(    
    @NotNull(message = "User ID is required")
    Long userId,

    @NotNull(message = "Concert ID is required")
    Long concertId
) {}
