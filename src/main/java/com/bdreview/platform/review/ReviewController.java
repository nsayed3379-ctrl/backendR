package com.bdreview.platform.review;

import com.bdreview.platform.common.CurrentUser;
import com.bdreview.platform.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> submit(@Valid @RequestBody SubmitReviewRequest request) {
        Review review = reviewService.submit(CurrentUser.id(), request);
        return ResponseEntity.ok(toResponse(review));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> edit(@PathVariable UUID id, @Valid @RequestBody UpdateReviewRequest request) {
        Review review = reviewService.edit(CurrentUser.id(), id, request);
        return ResponseEntity.ok(toResponse(review));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        reviewService.delete(CurrentUser.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<Void> vote(@PathVariable UUID id, @Valid @RequestBody VoteRequest request) {
        reviewService.vote(CurrentUser.id(), id, request.voteType());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/business/{businessId}")
    public ResponseEntity<PageResponse<ReviewResponse>> listForBusiness(
            @PathVariable UUID businessId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var results = reviewService.listForBusiness(businessId, page, size).map(this::toResponse);
        return ResponseEntity.ok(PageResponse.of(results));
    }

    @GetMapping("/business/{businessId}/dashboard")
    public ResponseEntity<PageResponse<ReviewResponse>> ownerDashboard(
            @PathVariable UUID businessId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var results = reviewService.ownerDashboardList(businessId, page, size).map(this::toResponse);
        return ResponseEntity.ok(PageResponse.of(results));
    }

    @GetMapping("/business/{businessId}/rating-trend")
    public ResponseEntity<List<Object[]>> ratingTrend(@PathVariable UUID businessId,
                                                        @RequestParam(defaultValue = "week") String bucket) {
        return ResponseEntity.ok(reviewService.ratingTrend(businessId, bucket));
    }

    @GetMapping("/mine")
    public ResponseEntity<PageResponse<ReviewResponse>> mine(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var results = reviewService.myReviews(CurrentUser.id(), page, size).map(this::toResponse);
        return ResponseEntity.ok(PageResponse.of(results));
    }

    private ReviewResponse toResponse(Review review) {
        List<String> photoUrls = reviewService.photosFor(review.getId()).stream().map(ReviewPhoto::getUrl).toList();
        return ReviewResponse.from(review, photoUrls);
    }
}
