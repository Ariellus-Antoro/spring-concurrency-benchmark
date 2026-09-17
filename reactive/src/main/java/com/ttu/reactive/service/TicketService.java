package com.ttu.reactive.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ttu.reactive.dto.CheckoutRequest;
import com.ttu.reactive.dto.CheckoutResponse;
import com.ttu.reactive.dto.SeatAvailabilityResponse;
import com.ttu.reactive.entity.Ticket;
import com.ttu.reactive.repository.ConcertRepository;
import com.ttu.reactive.repository.TicketRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service 
@RequiredArgsConstructor 
public class TicketService {

    private final ConcertRepository concertRepository;
    private final TicketRepository ticketRepository;
    private final TokenGenerationService tokenGenerationService;

    public Mono<SeatAvailabilityResponse> checkAvailability(Long concertId){
        return concertRepository.findById(concertId)
            .map(concert -> new SeatAvailabilityResponse(
                    concert.getId(),
                    concert.getName(), 
                    concert.getAvailableSeats()
                )).switchIfEmpty(Mono.error(new RuntimeException("Concert not found")));
    }

    @Transactional 
    public Mono<CheckoutResponse> checkoutTicket(CheckoutRequest request){
        return concertRepository.findById(request.userId())
            .switchIfEmpty(Mono.error(new RuntimeException("Concert not found")))
            .flatMap(concert ->{
                if(concert.getAvailableSeats() <= 0){
                    return Mono.error(new RuntimeException("Tickets are sold out !"));
                }
                concert.setAvailableSeats(concert.getAvailableSeats() - 1);

                return concertRepository.save(concert)
                        .then(tokenGenerationService.generateToken(request.userId(), concert.getId()))
                        .flatMap(token ->{
                            Ticket ticket = Ticket.builder()
                                    .concertId(concert.getId())
                                    .userId(request.userId())
                                    .bookingTime(LocalDateTime.now())
                                    .status("CONFIRMED")
                                    .token(token)
                                    .build();
                            return ticketRepository.save(ticket).map(savedTicket -> new CheckoutResponse(savedTicket.getId(), "SUCCESS", token));
                        });
            
            });
    }

}
