package com.ttu.reactive.entity;


import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

import lombok.*;

@Table("tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {
    @Id
    private Long id;

    @Column("concert_id")
    private Long concertId;

    @Column("user_id")
    private Long userId;

    @Column("booking_time")
    private LocalDateTime bookingTime;

    private String status;
    private String token;
}
