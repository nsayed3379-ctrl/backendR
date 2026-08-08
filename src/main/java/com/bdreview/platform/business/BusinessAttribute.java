package com.bdreview.platform.business;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** Lookup table for filterable attributes: parking, wifi, delivery, home_service, wheelchair_accessible, ... */
@Entity
@Table(name = "business_attribute")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessAttribute {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;
}
