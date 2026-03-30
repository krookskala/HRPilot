package com.hrpilot.backend.dashboard;

import com.hrpilot.backend.audit.AuditLog;
import com.hrpilot.backend.audit.AuditLogRepository;
import com.hrpilot.backend.dashboard.dto.DashboardResponse;
import com.hrpilot.backend.dashboard.dto.DashboardResponse.*;
import com.hrpilot.backend.department.DepartmentScopeService;
import com.hrpilot.backend.department.DepartmentRepository;
import com.hrpilot.backend.employee.Employee;
import com.hrpilot.backend.employee.EmployeeRepository;
import com.hrpilot.backend.leave.LeaveBalanceRepository;
import com.hrpilot.backend.leave.LeaveRequest;
import com.hrpilot.backend.leave.LeaveRequestRepository;
import com.hrpilot.backend.leave.LeaveStatus;
import com.hrpilot.backend.notification.Notification;
import com.hrpilot.backend.notification.NotificationRepository;
import com.hrpilot.backend.payroll.PayrollRecord;
import com.hrpilot.backend.payroll.PayrollRepository;
import com.hrpilot.backend.payroll.PayrollStatus;
import com.hrpilot.backend.user.CurrentUserService;
import com.hrpilot.backend.user.Role;
import com.hrpilot.backend.user.User;
import com.hrpilot.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final DepartmentScopeService departmentScopeService;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PayrollRepository payrollRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public DashboardResponse getDashboardData() {
        User currentUser = currentUserService.getCurrentUserEntity();
        Employee currentEmployee = employeeRepository.findByUserIdWithDepartment(currentUser.getId()).orElse(null);
        List<Long> managedDepartmentIds = departmentScopeService.getManagedDepartmentIds(currentUser.getId()).stream().toList();
        List<Employee> managedEmployees = managedDepartmentIds.isEmpty()
            ? List.of()
            : employeeRepository.findByDepartmentIdIn(managedDepartmentIds);
        Set<Long> managedEmployeeIds = managedEmployees.stream()
            .map(Employee::getId)
            .collect(java.util.stream.Collectors.toSet());

        Map<LeaveStatus, Long> leaveCounts = fetchLeaveCounts(currentUser, currentEmployee, managedEmployeeIds);
        Map<PayrollStatus, Object[]> payrollStats = fetchPayrollStats(currentUser, currentEmployee, managedEmployeeIds);
        long unreadNotifications = notificationRepository.countByUserIdAndReadAtIsNull(currentUser.getId());

        return new DashboardResponse(
            currentUser.getRole().name(),
            buildHeadline(currentUser.getRole()),
            buildSubheadline(currentUser.getRole(), managedDepartmentIds.size()),
            buildKeyMetrics(currentUser, currentEmployee, managedDepartmentIds, managedEmployeeIds, leaveCounts, payrollStats, unreadNotifications),
            buildRecentActivities(currentUser, currentEmployee, managedEmployeeIds),
            buildLeaveOverview(leaveCounts),
            buildPayrollOverview(payrollStats),
            new NotificationOverviewDto(unreadNotifications),
            buildTeamOverview(currentUser, managedDepartmentIds, managedEmployees, managedEmployeeIds, leaveCounts, payrollStats),
            buildPersonalOverview(currentUser, currentEmployee, leaveCounts, payrollStats, unreadNotifications),
            buildAuditOverview(currentUser),
            buildDepartmentDistribution(currentUser)
        );
    }

    private Map<LeaveStatus, Long> fetchLeaveCounts(User currentUser, Employee currentEmployee, Set<Long> managedEmployeeIds) {
        List<Object[]> rows;
        if (currentUser.getRole() == Role.EMPLOYEE && currentEmployee != null) {
            rows = leaveRequestRepository.countGroupByStatusForEmployees(List.of(currentEmployee.getId()));
        } else if (currentUser.getRole() == Role.DEPARTMENT_MANAGER && !managedEmployeeIds.isEmpty()) {
            rows = leaveRequestRepository.countGroupByStatusForEmployees(managedEmployeeIds);
        } else {
            rows = leaveRequestRepository.countGroupByStatus();
        }
        Map<LeaveStatus, Long> result = new EnumMap<>(LeaveStatus.class);
        for (LeaveStatus s : LeaveStatus.values()) result.put(s, 0L);
        for (Object[] row : rows) {
            result.put((LeaveStatus) row[0], (Long) row[1]);
        }
        return result;
    }

    private Map<PayrollStatus, Object[]> fetchPayrollStats(User currentUser, Employee currentEmployee, Set<Long> managedEmployeeIds) {
        List<Object[]> rows;
        if (currentUser.getRole() == Role.EMPLOYEE && currentEmployee != null) {
            rows = payrollRepository.countAndSumGroupByStatusForEmployees(List.of(currentEmployee.getId()));
        } else if (currentUser.getRole() == Role.DEPARTMENT_MANAGER && !managedEmployeeIds.isEmpty()) {
            rows = payrollRepository.countAndSumGroupByStatusForEmployees(managedEmployeeIds);
        } else {
            rows = payrollRepository.countAndSumGroupByStatus();
        }
        Map<PayrollStatus, Object[]> result = new EnumMap<>(PayrollStatus.class);
        for (PayrollStatus s : PayrollStatus.values()) result.put(s, new Object[]{0L, BigDecimal.ZERO});
        for (Object[] row : rows) {
            result.put((PayrollStatus) row[0], new Object[]{row[1], row[2]});
        }
        return result;
    }

    private long getPayrollCount(Map<PayrollStatus, Object[]> stats, PayrollStatus status) {
        return (Long) stats.get(status)[0];
    }

    private BigDecimal getPayrollSum(Map<PayrollStatus, Object[]> stats, PayrollStatus status) {
        return (BigDecimal) stats.get(status)[1];
    }

    private String buildHeadline(Role role) {
        return switch (role) {
            case ADMIN -> "Company Command Center";
            case HR_MANAGER -> "HR Operations Hub";
            case DEPARTMENT_MANAGER -> "Team Management Desk";
            case EMPLOYEE -> "Personal Work Hub";
        };
    }

    private String buildSubheadline(Role role, int managedDepartmentCount) {
        return switch (role) {
            case ADMIN -> "Monitor workforce growth, payroll throughput, and operational risk from one place.";
            case HR_MANAGER -> "Stay on top of approvals, onboarding gaps, and payroll release health.";
            case DEPARTMENT_MANAGER -> "Manage " + managedDepartmentCount + " department(s) with a live view of team leave and payroll activity.";
            case EMPLOYEE -> "Track your leave balance, payslips, notifications, and profile completeness.";
        };
    }

    private List<MetricCardDto> buildKeyMetrics(User currentUser, Employee currentEmployee, List<Long> managedDepartmentIds, Set<Long> managedEmployeeIds,
                                                  Map<LeaveStatus, Long> leaveCounts, Map<PayrollStatus, Object[]> payrollStats, long unreadNotifications) {
        return switch (currentUser.getRole()) {
            case ADMIN -> List.of(
                metric("Employees", employeeRepository.count(), "people", "cyan"),
                metric("Departments", departmentRepository.count(), "apartment", "orange"),
                metric("Pending Leave", leaveCounts.getOrDefault(LeaveStatus.PENDING, 0L), "event_available", "green"),
                metric("Audit Events", auditLogRepository.count(), "shield", "slate")
            );
            case HR_MANAGER -> List.of(
                metric("Pending Approvals", leaveCounts.getOrDefault(LeaveStatus.PENDING, 0L), "pending_actions", "green"),
                metric("Draft Payrolls", getPayrollCount(payrollStats, PayrollStatus.DRAFT), "payments", "indigo"),
                metric("Inactive Accounts", userRepository.countByIsActive(false), "person_off", "orange"),
                metric("Unread Alerts", unreadNotifications, "notifications", "slate")
            );
            case DEPARTMENT_MANAGER -> List.of(
                metric("Managed Departments", managedDepartmentIds.size(), "domain", "orange"),
                metric("Team Members", managedEmployeeIds.size(), "groups", "cyan"),
                metric("Pending Team Leave", leaveCounts.getOrDefault(LeaveStatus.PENDING, 0L), "event_busy", "green"),
                metric("Paid Team Payrolls", getPayrollCount(payrollStats, PayrollStatus.PAID), "receipt_long", "indigo")
            );
            case EMPLOYEE -> List.of(
                metric("Available Leave", currentEmployee == null ? 0 : availableLeaveDays(currentEmployee.getId()), "beach_access", "green"),
                metric("Pending Requests", leaveCounts.getOrDefault(LeaveStatus.PENDING, 0L), "hourglass_top", "orange"),
                metric("Payslips Ready", getPayrollCount(payrollStats, PayrollStatus.PUBLISHED) + getPayrollCount(payrollStats, PayrollStatus.PAID), "payments", "indigo"),
                metric("Unread Alerts", unreadNotifications, "notifications", "slate")
            );
        };
    }

    private LeaveOverviewDto buildLeaveOverview(Map<LeaveStatus, Long> leaveCounts) {
        return new LeaveOverviewDto(
            leaveCounts.getOrDefault(LeaveStatus.PENDING, 0L),
            leaveCounts.getOrDefault(LeaveStatus.APPROVED, 0L),
            leaveCounts.getOrDefault(LeaveStatus.REJECTED, 0L),
            leaveCounts.getOrDefault(LeaveStatus.CANCELLED, 0L)
        );
    }

    private PayrollOverviewDto buildPayrollOverview(Map<PayrollStatus, Object[]> payrollStats) {
        return new PayrollOverviewDto(
            getPayrollCount(payrollStats, PayrollStatus.DRAFT),
            getPayrollCount(payrollStats, PayrollStatus.PUBLISHED),
            getPayrollCount(payrollStats, PayrollStatus.PAID),
            getPayrollSum(payrollStats, PayrollStatus.PAID)
        );
    }

    private TeamOverviewDto buildTeamOverview(User currentUser, List<Long> managedDepartmentIds, List<Employee> managedEmployees, Set<Long> managedEmployeeIds,
                                               Map<LeaveStatus, Long> leaveCounts, Map<PayrollStatus, Object[]> payrollStats) {
        if (currentUser.getRole() != Role.DEPARTMENT_MANAGER) {
            return null;
        }

        return new TeamOverviewDto(
            managedDepartmentIds.size(),
            managedEmployees.size(),
            leaveCounts.getOrDefault(LeaveStatus.PENDING, 0L),
            leaveCounts.getOrDefault(LeaveStatus.APPROVED, 0L),
            getPayrollCount(payrollStats, PayrollStatus.PAID)
        );
    }

    private PersonalOverviewDto buildPersonalOverview(User currentUser, Employee currentEmployee,
                                                       Map<LeaveStatus, Long> leaveCounts, Map<PayrollStatus, Object[]> payrollStats, long unreadNotifications) {
        if (currentUser.getRole() != Role.EMPLOYEE || currentEmployee == null) {
            return null;
        }

        long totalPayslips = getPayrollCount(payrollStats, PayrollStatus.DRAFT)
            + getPayrollCount(payrollStats, PayrollStatus.PUBLISHED)
            + getPayrollCount(payrollStats, PayrollStatus.PAID);

        return new PersonalOverviewDto(
            leaveCounts.getOrDefault(LeaveStatus.PENDING, 0L),
            leaveCounts.getOrDefault(LeaveStatus.APPROVED, 0L),
            availableLeaveDays(currentEmployee.getId()),
            totalPayslips,
            unreadNotifications,
            profileCompletion(currentEmployee, currentUser)
        );
    }

    private AuditOverviewDto buildAuditOverview(User currentUser) {
        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.HR_MANAGER) {
            return null;
        }

        return new AuditOverviewDto(
            auditLogRepository.count(),
            auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5)).getNumberOfElements()
        );
    }

    private List<RecentActivityDto> buildRecentActivities(User currentUser, Employee currentEmployee, Set<Long> managedEmployeeIds) {
        return switch (currentUser.getRole()) {
            case ADMIN, HR_MANAGER -> buildOrganizationActivities();
            case DEPARTMENT_MANAGER -> buildTeamActivities(managedEmployeeIds);
            case EMPLOYEE -> buildEmployeeActivities(currentUser, currentEmployee);
        };
    }

    private List<RecentActivityDto> buildOrganizationActivities() {
        List<ActivityCandidate> activities = new ArrayList<>();
        PageRequest recentRequest = PageRequest.of(0, 4, Sort.by(Sort.Direction.DESC, "createdAt"));

        employeeRepository.findAll(recentRequest).getContent().forEach(employee ->
            activities.add(new ActivityCandidate(
                "EMPLOYEE",
                "New employee joined: " + employee.getFirstName() + " " + employee.getLastName(),
                employee.getCreatedAt()
            ))
        );

        leaveRequestRepository.findRecentWithEmployee(recentRequest).getContent().forEach(leave ->
            activities.add(new ActivityCandidate(
                "LEAVE",
                "Leave request for " + leave.getEmployee().getFirstName() + " is " + leave.getStatus(),
                leave.getCreatedAt()
            ))
        );

        payrollRepository.findAll(recentRequest).getContent().forEach(payroll ->
            activities.add(new ActivityCandidate(
                "PAYROLL",
                "Payroll " + payroll.getStatus() + " for " + payroll.getEmployee().getFirstName() + " " + payroll.getEmployee().getLastName(),
                payroll.getPaidAt() != null ? payroll.getPaidAt() : payroll.getPublishedAt() != null ? payroll.getPublishedAt() : payroll.getCreatedAt()
            ))
        );

        auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 3)).getContent().forEach(audit ->
            activities.add(new ActivityCandidate(
                "AUDIT",
                audit.getSummary(),
                audit.getCreatedAt()
            ))
        );

        return toActivityDtos(activities);
    }

    private List<RecentActivityDto> buildTeamActivities(Set<Long> managedEmployeeIds) {
        if (managedEmployeeIds.isEmpty()) {
            return List.of();
        }

        List<ActivityCandidate> activities = new ArrayList<>();
        leaveRequestRepository.findByEmployeeIdInWithEmployee(managedEmployeeIds).stream()
            .limit(5)
            .forEach(leave -> activities.add(new ActivityCandidate(
                "LEAVE",
                leave.getEmployee().getFirstName() + " " + leave.getEmployee().getLastName() + " leave is " + leave.getStatus(),
                leave.getCreatedAt()
            )));

        payrollRepository.findByEmployeeIdInOrderByCreatedAtDesc(managedEmployeeIds).stream()
            .limit(5)
            .forEach(payroll -> activities.add(new ActivityCandidate(
                "PAYROLL",
                "Payroll " + payroll.getStatus() + " for " + payroll.getEmployee().getFirstName() + " " + payroll.getEmployee().getLastName(),
                payroll.getPaidAt() != null ? payroll.getPaidAt() : payroll.getPublishedAt() != null ? payroll.getPublishedAt() : payroll.getCreatedAt()
            )));

        return toActivityDtos(activities);
    }

    private List<RecentActivityDto> buildEmployeeActivities(User currentUser, Employee currentEmployee) {
        if (currentEmployee == null) {
            return List.of();
        }

        List<ActivityCandidate> activities = new ArrayList<>();
        leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(currentEmployee.getId()).stream()
            .limit(4)
            .forEach(leave -> activities.add(new ActivityCandidate(
                "LEAVE",
                "Your " + leave.getType() + " request is " + leave.getStatus(),
                leave.getCreatedAt()
            )));

        payrollRepository.findByEmployeeIdOrderByYearDescMonthDesc(currentEmployee.getId()).stream()
            .limit(4)
            .forEach(payroll -> activities.add(new ActivityCandidate(
                "PAYROLL",
                "Payroll for " + payroll.getMonth() + "/" + payroll.getYear() + " is " + payroll.getStatus(),
                payroll.getPaidAt() != null ? payroll.getPaidAt() : payroll.getPublishedAt() != null ? payroll.getPublishedAt() : payroll.getCreatedAt()
            )));

        notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId(), PageRequest.of(0, 4)).getContent()
            .forEach(notification -> activities.add(new ActivityCandidate(
                "NOTIFICATION",
                notification.getTitle(),
                notification.getCreatedAt()
            )));

        return toActivityDtos(activities);
    }

    private List<RecentActivityDto> toActivityDtos(List<ActivityCandidate> activities) {
        return activities.stream()
            .filter(activity -> activity.occurredAt() != null)
            .sorted(Comparator.comparing(ActivityCandidate::occurredAt).reversed())
            .limit(8)
            .map(activity -> new RecentActivityDto(
                activity.type(),
                activity.description(),
                activity.occurredAt().toString()
            ))
            .toList();
    }

    private long countScopedLeave(Collection<Long> employeeIds, LeaveStatus status) {
        if (employeeIds.isEmpty()) {
            return 0;
        }
        return leaveRequestRepository.countByEmployeeIdInAndStatus(employeeIds, status);
    }

    private long countScopedPayroll(Collection<Long> employeeIds, PayrollStatus status) {
        if (employeeIds.isEmpty()) {
            return 0;
        }
        return payrollRepository.countByEmployeeIdInAndStatus(employeeIds, status);
    }

    private int availableLeaveDays(Long employeeId) {
        return leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, LocalDate.now().getYear()).stream()
            .mapToInt(balance -> balance.getRemainingDays())
            .sum();
    }

    private int profileCompletion(Employee employee, User user) {
        int completedFields = 0;
        int totalFields = 6;
        if (hasText(employee.getFirstName())) {
            completedFields++;
        }
        if (hasText(employee.getLastName())) {
            completedFields++;
        }
        if (hasText(employee.getPosition())) {
            completedFields++;
        }
        if (employee.getDepartment() != null) {
            completedFields++;
        }
        if (hasText(employee.getPhotoUrl())) {
            completedFields++;
        }
        if (user.getActivatedAt() != null) {
            completedFields++;
        }
        return (completedFields * 100) / totalFields;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private List<DepartmentDistributionDto> buildDepartmentDistribution(User currentUser) {
        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.HR_MANAGER) {
            return null;
        }
        return employeeRepository.countGroupByDepartmentName().stream()
            .map(row -> new DepartmentDistributionDto((String) row[0], (Long) row[1]))
            .toList();
    }

    private MetricCardDto metric(String label, long value, String icon, String accent) {
        return new MetricCardDto(label, String.valueOf(value), icon, accent);
    }

    private record ActivityCandidate(
        String type,
        String description,
        LocalDateTime occurredAt
    ) {
    }
}
