package com.bdreview.platform.claim;

import com.bdreview.platform.common.CurrentUser;
import com.bdreview.platform.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/claims")
public class BusinessClaimController {

    private final BusinessClaimService claimService;

    public BusinessClaimController(BusinessClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping
    public ResponseEntity<BusinessClaim> fileClaim(@Valid @RequestBody FileClaimRequest request) {
        return ResponseEntity.ok(claimService.fileClaim(CurrentUser.id(), request));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<BusinessClaim>> mine() {
        return ResponseEntity.ok(claimService.myClaims(CurrentUser.id()));
    }

    @GetMapping("/queue")
    public ResponseEntity<PageResponse<BusinessClaim>> queue(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        CurrentUser.requireRole("ADMIN");
        return ResponseEntity.ok(PageResponse.of(claimService.queue(PageRequest.of(page, size))));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<BusinessClaim> resolve(@PathVariable UUID id, @Valid @RequestBody ResolveClaimRequest request) {
        return ResponseEntity.ok(claimService.resolve(id, request));
    }
}
