package com.ttu.reactive.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.ttu.reactive.entity.Ticket;

@Repository 
public interface TicketRepository extends ReactiveCrudRepository<Ticket, Long>{

}
