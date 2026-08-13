package com.bdreview.platform.review;

/** Business detail page "Overall rating" bar chart — count of public reviews per star value. */
public record RatingBreakdownResponse(
        int fiveStar,
        int fourStar,
        int threeStar,
        int twoStar,
        int oneStar,
        int total
) {
}
