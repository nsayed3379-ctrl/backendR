package com.bdreview.platform.fakereview;

import java.util.List;

public record FakeReviewAnalysisRequestDto(ReviewInputDto review, List<ReviewInputDto> recentReviews) {
}
