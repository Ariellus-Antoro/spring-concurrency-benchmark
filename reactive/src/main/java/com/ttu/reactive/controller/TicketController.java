package com.ttu.reactive.controller;

import com.ttu.reactive.dto.CheckoutRequest;
import com.ttu.reactive.dto.CheckoutResponse;
import com.ttu.reactive.dto.SeatAvailabilityResponse;
import com.ttu.reactive.service.TokenGenerationService;
import com.ttu.reactive.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("api/tickets")
@RequiredArgsConstructor 
public class TicketController {
    private final TicketService ticketService;
    private final TokenGenerationService tokenGenerationService;

    @GetMapping("/availability/{concertId}")
    public Mono<ResponseEntity<SeatAvailabilityResponse>> checkSeats(@PathVariable Long concertId){
        return ticketService.checkAvailability(concertId)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/checkout")
    public Mono<ResponseEntity<CheckoutResponse>> processCheckout(@Valid @RequestBody CheckoutRequest request){
        return ticketService.checkoutTicket(request).map(ResponseEntity::ok);
    }

    @PostMapping("/token")
    public Mono<ResponseEntity<String>>generateToken(@Valid @RequestBody CheckoutRequest request){
        return tokenGenerationService.generateToken(request.userId(), request.concertId()).map(ResponseEntity::ok);

    }

}
