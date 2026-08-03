package com.bdreview.platform.business;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AreaRepository extends JpaRepository<Area, UUID> {
    List<Area> findByCityId(UUID cityId);
    Optional<Area> findByCityIdAndNameIgnoreCase(UUID cityId, String name);
}
