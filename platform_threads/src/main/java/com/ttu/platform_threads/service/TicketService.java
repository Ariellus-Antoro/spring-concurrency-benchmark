package com.ttu.platform_threads.service;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.ttu.platform_threads.entity.Concert;
import com.ttu.platform_threads.repository.ConcertRepository;
import com.ttu.platform_threads.repository.TicketRepository;

import com.ttu.platform_threads.dto.SeatAvailabilityResponse;
import com.ttu.platform_threads.dto.CheckoutResponse;
import com.ttu.platform_threads.dto.CheckoutRequest;

import com.ttu.platform_threads.entity.Ticket;

import org.springframework.transaction.annotation.Transactional;


@Service 
@RequiredArgsConstructor
public class TicketService {
    private final ConcertRepository concertRepository;
    private final TicketRepository ticketRepository;
    private final TokenGenerationService tokenGenerationService;

    @Transactional(readOnly = true)
    public SeatAvailabilityResponse checkAvailability(Long concertId){
        Concert concert = concertRepository.findById(concertId).orElseThrow(()-> new RuntimeException("Concert not found"));

        return new SeatAvailabilityResponse(concert.getId(), concert.getName(), concert.getAvailableSeats());
    }

    @Transactional
    public CheckoutResponse checkoutTicket(CheckoutRequest request){
        Concert concert = concertRepository.findById(request.concertId()).orElseThrow(()-> new RuntimeException("Concert not found"));

        if (concert.getAvailableSeats() <= 0) {
            throw new RuntimeException("Tickets are sold out!");
        }

        concert.setAvailableSeats(concert.getAvailableSeats()-1);
        concertRepository.save(concert);

        String token = tokenGenerationService.generateEncryptedToken(request.userId(), concert.getId());

        Ticket ticket = Ticket.builder()
                .concertId(concert.getId())
                .userId(request.userId())
                .bookingTime(LocalDateTime.now())
                .status("CONFIRMED")
                .token(token)
                .build();
        ticketRepository.save(ticket);

        return new CheckoutResponse(ticket.getId(),"SUCCESS",token);
    }
}
