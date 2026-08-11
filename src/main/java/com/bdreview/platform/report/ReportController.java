package com.bdreview.platform.report;

import com.bdreview.platform.auth.User;
import com.bdreview.platform.auth.UserRepository;
import com.bdreview.platform.common.CurrentUser;
import com.bdreview.platform.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;

    public ReportController(ReportService reportService, UserRepository userRepository) {
        this.reportService = reportService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<Report> create(@Valid @RequestBody CreateReportRequest request) {
        return ResponseEntity.ok(reportService.create(CurrentUser.id(), request));
    }

    @GetMapping("/queue")
    public ResponseEntity<PageResponse<ReportResponse>> queue(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        CurrentUser.requireRole("ADMIN");
        var results = reportService.queue(PageRequest.of(page, size))
                .map(r -> ReportResponse.from(r, userRepository.findById(r.getReporterUserId())
                        .map(User::getName).orElse(null)));
        return ResponseEntity.ok(PageResponse.of(results));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<Void> resolve(@PathVariable UUID id, @Valid @RequestBody ResolveReportRequest request) {
        reportService.resolve(id, request.outcome(), request.resolutionNote());
        return ResponseEntity.noContent().build();
    }
}
