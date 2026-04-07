package com.hrpilot.backend.seed;

import com.hrpilot.backend.department.Department;
import com.hrpilot.backend.employee.Employee;
import com.hrpilot.backend.leave.LeaveRequest;
import com.hrpilot.backend.leave.LeaveRequestHistory;
import com.hrpilot.backend.user.Role;
import com.hrpilot.backend.user.User;

import java.util.Collection;
import java.util.List;

record DepartmentBlueprint(
    String key,
    String name,
    String parentKey,
    Role managerRole,
    int headcount,
    String managerFirstName,
    String managerLastName,
    PositionProfile managerProfile,
    List<PositionProfile> contributorProfiles
) {
}

record PositionProfile(String title, int minSalary, int maxSalary) {
}

record PersonName(String firstName, String lastName) {
}

record EmployeeSeedContext(
    Employee employee,
    User user,
    Department department,
    DepartmentBlueprint blueprint,
    String taxClass,
    boolean manager
) {
}

record DateWindow(java.time.LocalDate startDate, java.time.LocalDate endDate) {
    boolean overlaps(java.time.LocalDate otherStart, java.time.LocalDate otherEnd) {
        return !otherEnd.isBefore(startDate) && !otherStart.isAfter(endDate);
    }
}

record LeaveScenario(LeaveRequest leaveRequest, Collection<LeaveRequestHistory> historyEntries) {
}
