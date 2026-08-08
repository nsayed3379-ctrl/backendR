package com.bdreview.platform.messaging;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** businessId identifies the thread; direction (consumer->owner or owner->consumer) is inferred from the sender's role. */
public record SendMessageRequest(@NotNull UUID businessId, @NotBlank String content) {
}
