package com.ttu.reactive.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.ttu.reactive.entity.Concert;

@Repository 
public interface ConcertRepository extends ReactiveCrudRepository<Concert, Long> {

}
