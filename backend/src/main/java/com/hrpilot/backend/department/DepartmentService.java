package com.hrpilot.backend.department;

import com.hrpilot.backend.user.User;
import com.hrpilot.backend.user.UserRepository;
import com.hrpilot.backend.department.dto.DepartmentResponse;
import com.hrpilot.backend.department.dto.CreateDepartmentRequest;
import com.hrpilot.backend.common.exception.BusinessRuleException;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.common.exception.DuplicateResourceException;
import com.hrpilot.backend.employee.EmployeeRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        log.info("Creating department with name: {}", request.name());
        if (departmentRepository.existsByName(request.name())) {
            log.warn("Department creation failed - name already exists: {}", request.name());
            throw new DuplicateResourceException("Department", "name", request.name());
        }
        Department department = Department.builder()
            .name(request.name())
            .build();

        if (request.managerId() != null) {
            User manager = userRepository.findById(request.managerId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.managerId()));
            department.setManager(manager);
        }

        if (request.parentDepartmentId() != null) {
            Department parent = 
        departmentRepository.findById(request.parentDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.parentDepartmentId()));
            department.setParentDepartment(parent);
        }

        Department savedDepartment = departmentRepository.save(department);
        log.info("Department created successfully with id: {}", savedDepartment.getId());
        return toResponse(savedDepartment);
    }

    public Page<DepartmentResponse> getAllDepartments(Pageable pageable) {
        return departmentRepository.findAll(pageable)
            .map(dept -> toResponse(dept));
    }

    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        return toResponse(department);
    }

    public void deleteDepartment(Long id) {
        log.info("Deleting department with id: {}", id);
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Department", "id", id);
        }
        if (!departmentRepository.findByParentDepartmentId(id).isEmpty()) {
            throw new BusinessRuleException("Cannot delete department with sub-departments");
        }
        if (employeeRepository.existsByDepartmentId(id)) {
            throw new BusinessRuleException("Cannot delete department with assigned employees");
        }
        departmentRepository.deleteById(id);
        log.info("Department deleted successfully with id: {}", id);
    }

    private DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
            department.getId(),
            department.getName(),
            department.getManager() != null ?
        department.getManager().getEmail() : null,
            department.getParentDepartment() != null ?
        department.getParentDepartment().getId() : null,
            department.getParentDepartment() != null ?
        department.getParentDepartment().getName() : null
        );
    }
}