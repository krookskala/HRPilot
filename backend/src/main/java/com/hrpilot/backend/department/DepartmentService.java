package com.hrpilot.backend.department;

import com.hrpilot.backend.user.User;
import com.hrpilot.backend.user.UserRepository;
import com.hrpilot.backend.department.dto.DepartmentResponse;
import com.hrpilot.backend.department.dto.CreateDepartmentRequest;
import com.hrpilot.backend.department.dto.UpdateDepartmentRequest;
import com.hrpilot.backend.common.exception.BusinessRuleException;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.common.exception.DuplicateResourceException;
import com.hrpilot.backend.employee.EmployeeRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    public DepartmentResponse updateDepartment(Long id, UpdateDepartmentRequest request) {
        log.info("Updating department with id: {}", id);
        Department department = departmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        // Check duplicate name (only if changed)
        if (!department.getName().equals(request.name()) && departmentRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Department", "name", request.name());
        }
        department.setName(request.name());

        // Update manager
        if (request.managerId() != null) {
            User manager = userRepository.findById(request.managerId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.managerId()));
            department.setManager(manager);
        } else {
            department.setManager(null);
        }

        // Update parent department with circular hierarchy check
        if (request.parentDepartmentId() != null) {
            if (request.parentDepartmentId().equals(id)) {
                throw new BusinessRuleException("A department cannot be its own parent");
            }
            Department parent = departmentRepository.findById(request.parentDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.parentDepartmentId()));

            // Check circular hierarchy
            Department current = parent;
            while (current.getParentDepartment() != null) {
                if (current.getParentDepartment().getId().equals(id)) {
                    throw new BusinessRuleException("Circular department hierarchy detected");
                }
                current = current.getParentDepartment();
            }
            department.setParentDepartment(parent);
        } else {
            department.setParentDepartment(null);
        }

        Department saved = departmentRepository.save(department);
        log.info("Department updated successfully with id: {}", id);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<DepartmentResponse> getAllDepartments(Pageable pageable) {
        return departmentRepository.findAll(pageable)
            .map(dept -> toResponse(dept));
    }

    @Transactional(readOnly = true)
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