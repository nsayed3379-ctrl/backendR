package com.bdreview.platform.business;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "area", uniqueConstraints = @UniqueConstraint(columnNames = {"city_id", "name"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Area {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    @JsonIgnore
    private City city;

    @Column(nullable = false, length = 100)
    private String name;
}
