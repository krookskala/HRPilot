package com.hrpilot.backend.department;

import com.hrpilot.backend.user.User;
import com.hrpilot.backend.user.UserRepository;
import com.hrpilot.backend.department.dto.DepartmentResponse;
import com.hrpilot.backend.department.dto.CreateDepartmentRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        if (departmentRepository.existsByName(request.name())) {
            throw new RuntimeException("Department already exists");
        }
        Department department = Department.builder()
            .name(request.name())
            .build();

        if (request.managerId() != null) {
            User manager = userRepository.findById(request.managerId())
                .orElseThrow(() -> new RuntimeException("Manager Not Found"));
            department.setManager(manager);
        }

        if (request.parentDepartmentId() != null) {
            Department parent = 
        departmentRepository.findById(request.parentDepartmentId())
                .orElseThrow(() -> new RuntimeException("Parent Department Not Found"));
            department.setParentDepartment(parent);
        }

        Department savedDepartment = departmentRepository.save(department);
        return toResponse(savedDepartment);
    }

    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
            .map(dept -> toResponse(dept))
            .toList();
    }

    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Department Not Found"));
        return toResponse(department);
    }

    public void deleteDepartment(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new RuntimeException("Department Not Found");
        }
        departmentRepository.deleteById(id);
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