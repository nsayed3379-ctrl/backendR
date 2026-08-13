package com.bdreview.platform.business;

import jakarta.validation.constraints.NotNull;

public record BusinessReactionRequest(@NotNull BusinessReactionType reactionType) {
}