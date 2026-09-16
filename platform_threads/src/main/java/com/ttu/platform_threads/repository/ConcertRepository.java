package com.ttu.platform_threads.repository;

import org.springframework.stereotype.Repository;
import com.ttu.platform_threads.entity.Concert;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface ConcertRepository extends JpaRepository<Concert, Long> {

}
