package com.bdreview.platform.fakereview;

import java.util.List;
import java.util.UUID;

public record FakeReviewAnalysisResponseDto(
        UUID reviewId,
        int suspicionScore,
        String visibilityStatus,
        List<SignalResultDto> signals
) {
}
