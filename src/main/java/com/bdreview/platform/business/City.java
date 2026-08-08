package com.bdreview.platform.business;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "city")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class City {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;
}
