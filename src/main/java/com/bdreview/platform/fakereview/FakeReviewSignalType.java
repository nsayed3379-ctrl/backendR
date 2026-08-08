package com.bdreview.platform.fakereview;

/** The three MVP signals feeding a review's suspicion score (spec §14). */
public enum FakeReviewSignalType {
    CONTENT_PATTERN,   // LLM flags generic/templated language via multilingual embeddings (Bangla/Banglish-aware)
    TIMING_PATTERN,    // abnormally fast cluster of reviews on one business
    RATING_CLUSTERING  // sudden concentration of all 5-star or all 1-star reviews
}
