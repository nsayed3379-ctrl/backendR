package com.bdreview.platform.messaging;

import jakarta.validation.constraints.NotBlank;

public record ReplyRequest(@NotBlank String content) {
}
