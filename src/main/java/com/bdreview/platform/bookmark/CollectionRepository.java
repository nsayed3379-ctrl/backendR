package com.bdreview.platform.bookmark;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface CollectionRepository extends JpaRepository<Collection, UUID> {

    List<Collection> findByUserId(UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE Collection c SET c.name = :name WHERE c.id = :id AND c.userId = :userId")
    int rename(@Param("id") UUID id, @Param("userId") UUID userId, @Param("name") String name);
}
