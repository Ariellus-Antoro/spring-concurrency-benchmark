package com.ttu.platform_threads.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "concert_id")
    private Long concertId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "booking_time")
    private LocalDateTime bookingTime;

    private String status;
    private String token;

}
