package com.hrpilot.backend.employee;

import com.hrpilot.backend.employee.dto.CreateEmployeeRequest;
import com.hrpilot.backend.employee.dto.EmployeeResponse;
import com.hrpilot.backend.user.User;
import com.hrpilot.backend.user.UserRepository;
import com.hrpilot.backend.user.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private User buildUser() {
        return User.builder()
                .id(1L)
                .email("emp@test.com")
                .role(Role.EMPLOYEE)
                .build();
    }

    private Employee buildEmployee(User user) {
        return Employee.builder()
                .id(1L)
                .user(user)
                .firstName("John")
                .lastName("Doe")
                .position("Developer")
                .salary(new BigDecimal("5000"))
                .hireDate(LocalDate.of(2024, 1, 15))
                .build();
    }

    @Test
    void createEmployee_success() {
        // Arrange
        User user = buildUser();
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                1L, "John", "Doe", "Developer",
                new BigDecimal("5000"), LocalDate.of(2024, 1, 15));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        Employee savedEmployee = buildEmployee(user);
        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);

        // Act
        EmployeeResponse response = employeeService.createEmployee(request);

        // Assert
        assertNotNull(response);
        assertEquals("John", response.firstName());
        assertEquals("Doe", response.lastName());
        assertEquals("emp@test.com", response.email());
    }

    @Test
    void createEmployee_userNotFound_throwsException() {
        // Arrange
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                99L, "John", "Doe", "Developer",
                new BigDecimal("5000"), LocalDate.of(2024, 1, 15));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> employeeService.createEmployee(request));
    }

    @Test
    void getAllEmployees_returnsAllEmployees() {
        // Arrange
        User user = buildUser();
        Employee employee = buildEmployee(user);
        when(employeeRepository.findAll()).thenReturn(List.of(employee));

        // Act
        List<EmployeeResponse> responses = employeeService.getAllEmployees();

        // Assert
        assertEquals(1, responses.size());
        assertEquals("John", responses.get(0).firstName());
    }

    @Test
    void getEmployeeById_employeeExists_returnsResponse() {
        // Arrange
        User user = buildUser();
        Employee employee = buildEmployee(user);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        // Act
        EmployeeResponse response = employeeService.getEmployeeById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("John", response.firstName());
    }

    @Test
    void getEmployeeById_employeeNotFound_throwsException() {
        // Arrange
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> employeeService.getEmployeeById(99L));
    }

    @Test
    void deleteEmployee_employeeExists_deletesSuccessfully() {
        // Arrange
        when(employeeRepository.existsById(1L)).thenReturn(true);

        // Act
        employeeService.deleteEmployee(1L);

        // Assert
        verify(employeeRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteEmployee_employeeNotFound_throwsException() {
        // Arrange
        when(employeeRepository.existsById(99L)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> employeeService.deleteEmployee(99L));
    }
}
