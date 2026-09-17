package com.ttu.virtual_threads.service;

import com.ttu.virtual_threads.dto.CheckoutRequest;
import com.ttu.virtual_threads.dto.CheckoutResponse;
import com.ttu.virtual_threads.dto.SeatAvailabilityResponse;
import com.ttu.virtual_threads.entity.Concert;
import com.ttu.virtual_threads.entity.Ticket;
import com.ttu.virtual_threads.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class TicketService {
    private final ConcertRepository concertRepository;
    private final TicketRepository ticketRepository;
    private final TokenGenerationService tokenGenerationService;

    @Transactional(readOnly = true)
    public SeatAvailabilityResponse checkAvailability(Long concertId) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new RuntimeException("Concert not found"));
        
        return new SeatAvailabilityResponse(
                concert.getId(), 
                concert.getName(), 
                concert.getAvailableSeats()
        );
    }

    @Transactional 
    public CheckoutResponse checkoutTicket(CheckoutRequest request){
        Concert concert = concertRepository.findById(request.concertId())
                .orElseThrow(() -> new RuntimeException("Concert not found"));

        if (concert.getAvailableSeats() <= 0) {
            throw new RuntimeException("Tickets are sold out!");
        }

        concert.setAvailableSeats(concert.getAvailableSeats() - 1);
        concertRepository.save(concert);

        String token = tokenGenerationService.generateToken(request.userId(), concert.getId());

        Ticket ticket = Ticket.builder()
                .concertId(concert.getId())
                .userId(request.userId())
                .bookingTime(LocalDateTime.now())
                .status("CONFIRMED")
                .token(token)
                .build();
        
        ticketRepository.save(ticket);

        return new CheckoutResponse(ticket.getId(), "SUCCESS", token);
    }

}
