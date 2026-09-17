package com.ttu.reactive.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.*;



@Table("concerts")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class Concert {
    @Id 
    private Long id;

    private String name;

    @Column("total_seats")
    private Integer totalSeats;

    @Column("available_seats")
    private Integer availableSeats;

    @Version 
    private Long version;

}
