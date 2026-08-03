package com.bdreview.platform.messaging;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByThreadIdOrderByCreatedAtAsc(UUID threadId, Pageable pageable);

    @Modifying
    @Transactional
    @Query("""
            UPDATE Message m SET m.readAt = :now
            WHERE m.threadId = :threadId AND m.senderUserId <> :readerUserId AND m.readAt IS NULL
            """)
    int markThreadReadBy(@Param("threadId") UUID threadId,
                          @Param("readerUserId") UUID readerUserId,
                          @Param("now") Instant now);

    long countByThreadIdAndReadAtIsNullAndSenderUserIdNot(UUID threadId, UUID excludingSender);
}
