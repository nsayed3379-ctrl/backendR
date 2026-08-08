package com.bdreview.platform.bookmark;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {

    List<Bookmark> findByUserId(UUID userId);

    List<Bookmark> findByCollectionId(UUID collectionId);

    Optional<Bookmark> findByUserIdAndBusinessId(UUID userId, UUID businessId);

    void deleteByUserIdAndBusinessId(UUID userId, UUID businessId);

    /** Deleting a collection un-assigns its bookmarks back to the default bucket rather than orphaning them. */
    @Modifying
    @Transactional
    @Query("UPDATE Bookmark b SET b.collectionId = NULL WHERE b.collectionId = :collectionId")
    void unassignCollection(@Param("collectionId") UUID collectionId);
}
