package com.hrpilot.backend.employee;

import com.hrpilot.backend.user.UserRepository;
import com.hrpilot.backend.user.User;
import com.hrpilot.backend.employee.dto.CreateEmployeeRequest;
import com.hrpilot.backend.employee.dto.EmployeeResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        User user = userRepository.findById(request.userId()).orElseThrow(() -> new
        RuntimeException("User Not Found"));

        Employee employee = Employee.builder()
            .user(user)
            .firstName(request.firstName())
            .lastName(request.lastName())
            .position(request.position())
            .salary(request.salary())
            .hireDate(request.hireDate())
            .build();
        
        Employee savedEmployee = employeeRepository.save(employee);
        return toResponse(savedEmployee);
    }

    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll().stream()
        .map(emp -> toResponse(emp))
        .toList();
    }

    public EmployeeResponse getEmployeeById (Long id) {
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Employee Not Found"));
        return toResponse(employee);
    }

    public void deleteEmployee (Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new RuntimeException("Employee Not Found");
        }
        employeeRepository.deleteById(id);
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