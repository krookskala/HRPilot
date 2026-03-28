package com.hrpilot.backend.employee;

import com.hrpilot.backend.user.UserRepository;
import com.hrpilot.backend.user.User;
import com.hrpilot.backend.employee.dto.CreateEmployeeRequest;
import com.hrpilot.backend.employee.dto.EmployeeResponse;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

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

    public Page<EmployeeResponse> getAllEmployees(Pageable pageable) {
        return employeeRepository.findAll(pageable)
            .map(emp -> toResponse(emp));
    }

    public EmployeeResponse getEmployeeById (Long id) {
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        return toResponse(employee);
    }

    public void deleteEmployee (Long id) {
        log.info("Deleting employee with id: {}", id);
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employee", "id", id);
        }
        employeeRepository.deleteById(id);
        log.info("Employee deleted successfully with id: {}", id);
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
            employee.getPhotoUrl()
        );
    }
}