package com.hrpilot.backend.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
    String role,
    String headline,
    String subheadline,
    List<MetricCardDto> keyMetrics,
    List<RecentActivityDto> recentActivities,
    LeaveOverviewDto leaveOverview,
    PayrollOverviewDto payrollOverview,
    NotificationOverviewDto notificationOverview,
    TeamOverviewDto teamOverview,
    PersonalOverviewDto personalOverview,
    AuditOverviewDto auditOverview,
    List<DepartmentDistributionDto> departmentDistribution
) {
    public record MetricCardDto(
        String label,
        String value,
        String icon,
        String accent
    ) {}

    public record RecentActivityDto(
        String type,
        String description,
        String timestamp
    ) {}

    public record LeaveOverviewDto(
        long pending,
        long approved,
        long rejected,
        long cancelled
    ) {}

    public record PayrollOverviewDto(
        long draft,
        long published,
        long paid,
        BigDecimal totalNetSalary
    ) {}

    public record NotificationOverviewDto(
        long unreadNotifications
    ) {}

    public record TeamOverviewDto(
        long managedDepartments,
        long teamMembers,
        long pendingLeaveRequests,
        long approvedLeaveRequests,
        long paidPayrolls
    ) {}

    public record PersonalOverviewDto(
        long pendingLeaveRequests,
        long approvedLeaveRequests,
        int availableLeaveDays,
        long payrollRecords,
        long unreadNotifications,
        int profileCompletion
    ) {}

    public record AuditOverviewDto(
        long totalEvents,
        long recentEvents
    ) {}

    public record DepartmentDistributionDto(
        String department,
        long count
    ) {}
}
