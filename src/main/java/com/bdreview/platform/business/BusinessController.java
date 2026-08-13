package com.bdreview.platform.business;

import com.bdreview.platform.common.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @PostMapping
    public ResponseEntity<BusinessResponse> create(@Valid @RequestBody CreateBusinessRequest request) {
        CurrentUser.requireRole("BUSINESS_OWNER");
        return ResponseEntity.ok(businessService.create(CurrentUser.id(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BusinessResponse> update(@PathVariable UUID id,
                                                   @Valid @RequestBody UpdateBusinessRequest request) {
        return ResponseEntity.ok(businessService.update(CurrentUser.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        businessService.softDelete(CurrentUser.id(), id);
        return ResponseEntity.noContent().build();
    }

    /** Report workflow: owner's "next step" after a flag — notifies every admin, doesn't clear it. */
    @PostMapping("/{id}/flag/request-review")
    public ResponseEntity<Void> requestFlagReview(@PathVariable UUID id) {
        businessService.requestFlagReview(CurrentUser.id(), id);
        return ResponseEntity.accepted().build();
    }

    /** Business-card reaction (Like/Dislike/Love/Wow) — toggles on repeat calls with the same type. */
    @PostMapping("/{id}/react")
    public ResponseEntity<Void> react(@PathVariable UUID id, @Valid @RequestBody BusinessReactionRequest request) {
        businessService.react(CurrentUser.id(), id, request.reactionType());
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{slug}")
    public ResponseEntity<BusinessResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(businessService.getBySlug(slug));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<BusinessResponse>> mine() {
        return ResponseEntity.ok(businessService.myBusinesses(CurrentUser.id()));
    }

    /** §3 Search + Filter (GPS-enabled). All filters are optional/combinable. */
    @GetMapping("/search")
    public ResponseEntity<com.bdreview.platform.common.PageResponse<BusinessResponse>> search(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID areaId,
            @RequestParam(required = false) String priceTier,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusMeters,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var results = businessService.search(categoryId, areaId, priceTier, minRating, lat, lng, radiusMeters, sort, page, size);
        return ResponseEntity.ok(com.bdreview.platform.common.PageResponse.of(results));
    }
}