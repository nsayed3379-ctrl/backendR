package com.bdreview.platform.business;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BusinessAttributeRepository extends JpaRepository<BusinessAttribute, UUID> {
    List<BusinessAttribute> findByIdIn(List<UUID> ids);
}
