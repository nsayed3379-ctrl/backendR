package com.bdreview.platform.verification;

import com.bdreview.platform.common.CurrentUser;
import com.bdreview.platform.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/nid-verifications")
public class NidVerificationController {

    private final NidVerificationService service;

    public NidVerificationController(NidVerificationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<NidVerification> submit(@Valid @RequestBody SubmitNidRequest request) {
        return ResponseEntity.ok(service.submit(CurrentUser.id(), request));
    }

    @GetMapping("/business/{businessId}")
    public ResponseEntity<List<NidVerification>> history(@PathVariable UUID businessId) {
        return ResponseEntity.ok(service.history(businessId));
    }

    @GetMapping("/queue")
    public ResponseEntity<PageResponse<NidVerification>> queue(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        CurrentUser.requireRole("ADMIN");
        return ResponseEntity.ok(PageResponse.of(service.queue(PageRequest.of(page, size))));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<Void> resolve(@PathVariable UUID id, @Valid @RequestBody ResolveNidRequest request) {
        service.resolve(id, request);
        return ResponseEntity.noContent().build();
    }
}
