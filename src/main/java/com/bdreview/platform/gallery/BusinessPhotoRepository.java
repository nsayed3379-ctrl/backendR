package com.bdreview.platform.gallery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BusinessPhotoRepository extends JpaRepository<BusinessPhoto, UUID> {

    /** 5-10 photos per business beyond the cover photo (spec §13); returned in display order. */
    List<BusinessPhoto> findByBusinessIdOrderBySortOrderAsc(UUID businessId);

    /** Batched gallery lookup for listing pages (search/mine) — avoids one query per business. */
    List<BusinessPhoto> findByBusinessIdInOrderBySortOrderAsc(List<UUID> businessIds);

    long countByBusinessId(UUID businessId);

    void deleteByIdAndBusinessId(UUID id, UUID businessId);
}
