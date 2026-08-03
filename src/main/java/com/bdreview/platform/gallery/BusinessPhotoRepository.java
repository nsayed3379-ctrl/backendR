package com.bdreview.platform.gallery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BusinessPhotoRepository extends JpaRepository<BusinessPhoto, UUID> {

    /** 5-10 photos per business beyond the cover photo (spec §13); returned in display order. */
    List<BusinessPhoto> findByBusinessIdOrderBySortOrderAsc(UUID businessId);

    long countByBusinessId(UUID businessId);

    void deleteByIdAndBusinessId(UUID id, UUID businessId);
}
