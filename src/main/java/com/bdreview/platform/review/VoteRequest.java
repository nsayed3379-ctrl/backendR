package com.bdreview.platform.review;

import jakarta.validation.constraints.NotNull;

public record VoteRequest(@NotNull VoteType voteType) {
}
