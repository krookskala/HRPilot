package com.hrpilot.backend.dashboard;

import com.hrpilot.backend.dashboard.dto.DashboardResponse;
import com.hrpilot.backend.dashboard.dto.DashboardResponse.*;
import com.hrpilot.backend.department.DepartmentRepository;
import com.hrpilot.backend.employee.Employee;
import com.hrpilot.backend.employee.EmployeeRepository;
import com.hrpilot.backend.leave.LeaveRequest;
import com.hrpilot.backend.leave.LeaveRequestRepository;
import com.hrpilot.backend.leave.LeaveStatus;
import com.hrpilot.backend.payroll.PayrollRecord;
import com.hrpilot.backend.payroll.PayrollRepository;
import com.hrpilot.backend.payroll.PayrollStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PayrollRepository payrollRepository;

    public DashboardResponse getDashboardData() {
        CountsDto counts = new CountsDto(
            employeeRepository.count(),
            departmentRepository.count(),
            leaveRequestRepository.count(),
            payrollRepository.count()
        );

        LeaveOverviewDto leaveOverview = new LeaveOverviewDto(
            leaveRequestRepository.countByStatus(LeaveStatus.PENDING),
            leaveRequestRepository.countByStatus(LeaveStatus.APPROVED),
            leaveRequestRepository.countByStatus(LeaveStatus.REJECTED)
        );

        PayrollOverviewDto payrollOverview = new PayrollOverviewDto(
            payrollRepository.countByStatus(PayrollStatus.DRAFT),
            payrollRepository.countByStatus(PayrollStatus.PAID),
            payrollRepository.sumPaidNetSalary()
        );

        List<RecentActivityDto> recentActivities = buildRecentActivities();

        return new DashboardResponse(counts, recentActivities, leaveOverview, payrollOverview);
    }

    private List<RecentActivityDto> buildRecentActivities() {
        List<RecentActivityDto> activities = new ArrayList<>();
        var sort = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<Employee> recentEmployees = employeeRepository.findAll(sort).getContent();
        for (Employee e : recentEmployees) {
            activities.add(new RecentActivityDto(
                "EMPLOYEE",
                "New employee: " + e.getFirstName() + " " + e.getLastName(),
                e.getCreatedAt() != null ? e.getCreatedAt().toString() : ""
            ));
        }

        List<LeaveRequest> recentLeaves = leaveRequestRepository.findAll(sort).getContent();
        for (LeaveRequest l : recentLeaves) {
            activities.add(new RecentActivityDto(
                "LEAVE",
                "Leave request (" + l.getType() + "): " + l.getStatus(),
                l.getCreatedAt() != null ? l.getCreatedAt().toString() : ""
            ));
        }

        activities.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));
        return activities.size() > 10 ? activities.subList(0, 10) : activities;
    }
}
