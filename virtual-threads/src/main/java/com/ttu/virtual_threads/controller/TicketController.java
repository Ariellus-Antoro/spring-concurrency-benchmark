package com.ttu.virtual_threads.controller;

import com.ttu.virtual_threads.dto.CheckoutRequest;
import com.ttu.virtual_threads.dto.CheckoutResponse;
import com.ttu.virtual_threads.dto.SeatAvailabilityResponse;
import com.ttu.virtual_threads.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;
    private final TokenGenerationService tokenGenerationService;

    @GetMapping("/availability/{concertId}")
    public ResponseEntity<SeatAvailabilityResponse> checkSeats(@PathVariable Long concertId) {
        return ResponseEntity.ok(ticketService.checkAvailability(concertId));
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> processCheckout(@Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(ticketService.checkoutTicket(request));
    }

    @PostMapping("/token")
    public ResponseEntity<String> generateToken(@Valid @RequestBody CheckoutRequest request){
        return ResponseEntity.ok(tokenGenerationService.generateToken(request.userId(), request.concertId()));
    }
}
