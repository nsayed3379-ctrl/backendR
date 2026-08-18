package com.bdreview.platform.messaging;

import java.time.Instant;
import java.util.UUID;

public record MessageThreadResponse(
        UUID id, UUID consumerUserId, String consumerName, UUID businessId, Instant createdAt, long unreadCount
) {
    /** unreadCount is always relative to the caller — messages the other party sent that this reader hasn't opened yet. */
    public static MessageThreadResponse from(MessageThread t, String consumerName, long unreadCount) {
        return new MessageThreadResponse(t.getId(), t.getConsumerUserId(), consumerName, t.getBusinessId(), t.getCreatedAt(), unreadCount);
    }
}
