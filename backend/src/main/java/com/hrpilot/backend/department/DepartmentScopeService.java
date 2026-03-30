package com.hrpilot.backend.department;

import com.hrpilot.backend.employee.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DepartmentScopeService {

    private final DepartmentRepository departmentRepository;

    public Set<Long> getManagedDepartmentIds(Long managerUserId) {
        List<Department> allDepartments = departmentRepository.findAll();
        Set<Long> managedRoots = new HashSet<>();
        for (Department department : allDepartments) {
            if (department.getManager() != null && managerUserId.equals(department.getManager().getId())) {
                managedRoots.add(department.getId());
            }
        }

        if (managedRoots.isEmpty()) {
            return Set.of();
        }

        Set<Long> scopedDepartmentIds = new HashSet<>(managedRoots);
        boolean added;
        do {
            added = false;
            for (Department department : allDepartments) {
                if (department.getParentDepartment() != null
                    && scopedDepartmentIds.contains(department.getParentDepartment().getId())
                    && scopedDepartmentIds.add(department.getId())) {
                    added = true;
                }
            }
        } while (added);

        return Set.copyOf(scopedDepartmentIds);
    }

    public boolean isEmployeeInManagedScope(Employee employee, Long managerUserId) {
        return employee.getDepartment() != null
            && getManagedDepartmentIds(managerUserId).contains(employee.getDepartment().getId());
    }
}
