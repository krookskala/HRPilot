package com.hrpilot.backend.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
    CountsDto counts,
    List<RecentActivityDto> recentActivities,
    LeaveOverviewDto leaveOverview,
    PayrollOverviewDto payrollOverview
) {
    public record CountsDto(
        long employees,
        long departments,
        long leaveRequests,
        long payrollRecords
    ) {}

    public record RecentActivityDto(
        String type,
        String description,
        String timestamp
    ) {}

    public record LeaveOverviewDto(
        long pending,
        long approved,
        long rejected
    ) {}

    public record PayrollOverviewDto(
        long draft,
        long paid,
        BigDecimal totalNetSalary
    ) {}
}
