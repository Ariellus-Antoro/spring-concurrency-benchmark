package com.ttu.virtual_threads.repository;

import org.springframework.stereotype.Repository;
import com.ttu.virtual_threads.entity.Concert;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface ConcertRepository extends JpaRepository<Concert, Long> {

}
