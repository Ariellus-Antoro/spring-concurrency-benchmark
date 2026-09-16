package com.ttu.platform_threads.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ttu.platform_threads.dto.CheckoutRequest;
import com.ttu.platform_threads.dto.CheckoutResponse;
import com.ttu.platform_threads.dto.SeatAvailabilityResponse;
import com.ttu.platform_threads.service.TicketService;
import com.ttu.platform_threads.service.TokenGenerationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor 
public class TicketController {
    private final TicketService ticketService;
    private final TokenGenerationService tokenGenerationService;

    @GetMapping("/availability/{concertId}")
    public ResponseEntity<SeatAvailabilityResponse> checkSeats(@PathVariable Long concertId){
        return ResponseEntity.ok(ticketService.checkAvailability(concertId));
    }

    @PostMapping ("/checkout")
    public ResponseEntity<CheckoutResponse> processCheckout(@RequestBody CheckoutRequest request){
        return ResponseEntity.ok(ticketService.checkoutTicket(request));
    }

    @PostMapping ("/token")
    public ResponseEntity<String> generateToken(@RequestBody CheckoutRequest request){
        return ResponseEntity.ok(tokenGenerationService.generateEncryptedToken(request.userId(), request.concertId()));
    }
}
