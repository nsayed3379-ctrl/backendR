package com.bdreview.platform.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** In-app inbox: SMS rows are a delivery record, not something shown in the web notification list. */
    Page<Notification> findByRecipientUserIdAndChannelOrderByCreatedAtDesc(
            UUID recipientUserId, NotificationChannel channel, Pageable pageable);

    long countByRecipientUserIdAndChannelAndStatusNot(
            UUID recipientUserId, NotificationChannel channel, NotificationStatus status);

    /** Ownership-checked lookup for the mark-as-read endpoint. */
    Optional<Notification> findByIdAndRecipientUserId(UUID id, UUID recipientUserId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.status = :status WHERE n.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") NotificationStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.status = :readStatus, n.readAt = :now WHERE n.id = :id")
    void markRead(@Param("id") UUID id, @Param("readStatus") NotificationStatus readStatus, @Param("now") Instant now);

    @Modifying
    @Transactional
    @Query("""
            UPDATE Notification n SET n.status = :readStatus, n.readAt = :now
            WHERE n.recipientUserId = :userId AND n.channel = :channel AND n.status <> :readStatus
            """)
    void markAllRead(@Param("userId") UUID userId, @Param("channel") NotificationChannel channel,
                      @Param("readStatus") NotificationStatus readStatus, @Param("now") Instant now);
}
