package com.hrpilot.backend.employee;

import com.hrpilot.backend.common.storage.FileStorageService;
import com.hrpilot.backend.department.Department;
import com.hrpilot.backend.department.DepartmentRepository;
import com.hrpilot.backend.user.UserRepository;
import com.hrpilot.backend.user.User;
import com.hrpilot.backend.employee.dto.CreateEmployeeRequest;
import com.hrpilot.backend.employee.dto.UpdateEmployeeRequest;
import com.hrpilot.backend.employee.dto.EmployeeResponse;
import com.hrpilot.backend.employee.dto.EmploymentHistoryResponse;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final EmploymentHistoryRepository historyRepository;
    private final FileStorageService fileStorageService;

    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        log.info("Creating employee for user id: {}", request.userId());
        User user = userRepository.findById(request.userId()).orElseThrow(() -> new
        ResourceNotFoundException("User", "id", request.userId()));

        Employee employee = Employee.builder()
            .user(user)
            .firstName(request.firstName())
            .lastName(request.lastName())
            .position(request.position())
            .salary(request.salary())
            .hireDate(request.hireDate())
            .build();

        Employee savedEmployee = employeeRepository.save(employee);
        log.info("Employee created successfully with id: {}", savedEmployee.getId());
        return toResponse(savedEmployee);
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, UpdateEmployeeRequest request) {
        log.info("Updating employee id: {}", id);
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));

        // Track position change
        if (request.position() != null && !request.position().equals(employee.getPosition())) {
            recordHistory(employee, "POSITION", employee.getPosition(), request.position());
            employee.setPosition(request.position());
        }

        // Track department change
        if (request.departmentId() != null) {
            Long oldDeptId = employee.getDepartment() != null ? employee.getDepartment().getId() : null;
            if (!request.departmentId().equals(oldDeptId)) {
                String oldDeptName = employee.getDepartment() != null ? employee.getDepartment().getName() : "None";
                Department newDept = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.departmentId()));
                recordHistory(employee, "DEPARTMENT", oldDeptName, newDept.getName());
                employee.setDepartment(newDept);
            }
        }

        // Track salary change
        if (request.salary() != null && request.salary().compareTo(employee.getSalary()) != 0) {
            recordHistory(employee, "SALARY", employee.getSalary().toPlainString(), request.salary().toPlainString());
            employee.setSalary(request.salary());
        }

        if (request.firstName() != null) employee.setFirstName(request.firstName());
        if (request.lastName() != null) employee.setLastName(request.lastName());

        Employee saved = employeeRepository.save(employee);
        log.info("Employee updated: {}", id);
        return toResponse(saved);
    }

    public List<EmploymentHistoryResponse> getEmploymentHistory(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee", "id", employeeId);
        }
        return historyRepository.findByEmployeeIdOrderByChangedAtDesc(employeeId).stream()
            .map(h -> new EmploymentHistoryResponse(h.getId(), h.getChangeType(),
                h.getOldValue(), h.getNewValue(), h.getChangedAt()))
            .toList();
    }

    public Page<EmployeeResponse> getAllEmployees(Pageable pageable) {
        return employeeRepository.findAll(pageable)
            .map(this::toResponse);
    }

    public Page<EmployeeResponse> searchEmployees(String search, Long departmentId,
                                                    String position, Pageable pageable) {
        Specification<Employee> spec = Specification.where(null);

        if (search != null && !search.isBlank()) {
            spec = spec.and(EmployeeSpecification.hasNameContaining(search));
        }
        if (departmentId != null) {
            spec = spec.and(EmployeeSpecification.hasDepartmentId(departmentId));
        }
        if (position != null && !position.isBlank()) {
            spec = spec.and(EmployeeSpecification.hasPosition(position));
        }

        return employeeRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        return toResponse(employee);
    }

    public EmployeeResponse uploadPhoto(Long id, MultipartFile file) {
        log.info("Uploading photo for employee id: {}", id);
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));

        if (employee.getPhotoUrl() != null) {
            fileStorageService.delete(employee.getPhotoUrl());
        }

        String photoPath = fileStorageService.store(file, "photos");
        employee.setPhotoUrl(photoPath);
        Employee saved = employeeRepository.save(employee);
        log.info("Photo uploaded for employee id: {}", id);
        return toResponse(saved);
    }

    public String exportToCsv() {
        List<Employee> employees = employeeRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("ID,First Name,Last Name,Email,Position,Department,Salary,Hire Date\n");

        for (Employee emp : employees) {
            sb.append(emp.getId()).append(",");
            sb.append(escapeCsv(emp.getFirstName())).append(",");
            sb.append(escapeCsv(emp.getLastName())).append(",");
            sb.append(escapeCsv(emp.getUser().getEmail())).append(",");
            sb.append(escapeCsv(emp.getPosition())).append(",");
            sb.append(escapeCsv(emp.getDepartment() != null ? emp.getDepartment().getName() : "")).append(",");
            sb.append(emp.getSalary()).append(",");
            sb.append(emp.getHireDate()).append("\n");
        }

        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public void deleteEmployee(Long id) {
        log.info("Deleting employee with id: {}", id);
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employee", "id", id);
        }
        employeeRepository.deleteById(id);
        log.info("Employee deleted successfully with id: {}", id);
    }

    private void recordHistory(Employee employee, String changeType, String oldValue, String newValue) {
        EmploymentHistory history = EmploymentHistory.builder()
            .employee(employee)
            .changeType(changeType)
            .oldValue(oldValue)
            .newValue(newValue)
            .build();
        historyRepository.save(history);
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
            employee.getId(),
            employee.getUser().getEmail(),
            employee.getFirstName(),
            employee.getLastName(),
            employee.getPosition(),
            employee.getSalary(),
            employee.getHireDate(),
            employee.getPhotoUrl(),
            employee.getDepartment() != null ? employee.getDepartment().getId() : null,
            employee.getDepartment() != null ? employee.getDepartment().getName() : null
        );
    }
}
