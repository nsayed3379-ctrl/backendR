package com.bdreview.platform.messaging;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id, UUID threadId, UUID senderUserId, String senderName, String content, Instant readAt, Instant createdAt
) {
    public static MessageResponse from(Message m, String senderName) {
        return new MessageResponse(m.getId(), m.getThreadId(), m.getSenderUserId(), senderName, m.getContent(),
                m.getReadAt(), m.getCreatedAt());
    }
}
