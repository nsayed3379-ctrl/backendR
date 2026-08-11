package com.bdreview.platform.report;

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

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<Report> create(@Valid @RequestBody CreateReportRequest request) {
        return ResponseEntity.ok(reportService.create(CurrentUser.id(), request));
    }

    @GetMapping("/queue")
    public ResponseEntity<PageResponse<Report>> queue(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        CurrentUser.requireRole("ADMIN");
        return ResponseEntity.ok(PageResponse.of(reportService.queue(PageRequest.of(page, size))));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<Void> resolve(@PathVariable UUID id, @Valid @RequestBody ResolveReportRequest request) {
        reportService.resolve(id, request.outcome(), request.resolutionNote());
        return ResponseEntity.noContent().build();
    }
}
