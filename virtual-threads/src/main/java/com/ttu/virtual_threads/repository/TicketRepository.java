package com.ttu.virtual_threads.repository;

import org.springframework.stereotype.Repository;

import com.ttu.virtual_threads.entity.Ticket;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long>{

}
