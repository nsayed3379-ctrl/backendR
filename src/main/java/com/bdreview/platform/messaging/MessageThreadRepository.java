package com.bdreview.platform.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageThreadRepository extends JpaRepository<MessageThread, UUID> {

    Optional<MessageThread> findByConsumerUserIdAndBusinessId(UUID consumerUserId, UUID businessId);

    /** Owner-side inbox: every thread opened against any of the owner's businesses. */
    List<MessageThread> findByBusinessIdIn(List<UUID> businessIds);

    List<MessageThread> findByConsumerUserId(UUID consumerUserId);
}
