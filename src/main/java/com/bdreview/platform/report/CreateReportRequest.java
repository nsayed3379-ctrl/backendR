package com.bdreview.platform.report;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateReportRequest(@NotNull ReportTargetType targetType, @NotNull UUID targetId, @NotNull ReportReason reason) {
}
