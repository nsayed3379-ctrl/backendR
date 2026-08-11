package com.bdreview.platform.messaging;

import java.time.Instant;
import java.util.UUID;

public record MessageThreadResponse(
        UUID id, UUID consumerUserId, String consumerName, UUID businessId, Instant createdAt
) {
    public static MessageThreadResponse from(MessageThread t, String consumerName) {
        return new MessageThreadResponse(t.getId(), t.getConsumerUserId(), consumerName, t.getBusinessId(), t.getCreatedAt());
    }
}
