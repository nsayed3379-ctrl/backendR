package com.bdreview.platform.business;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** Fixed taxonomy (electrician, restaurant, salon, ...) — deliberately not freeform. */
@Entity
@Table(name = "category")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Category {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;
}
