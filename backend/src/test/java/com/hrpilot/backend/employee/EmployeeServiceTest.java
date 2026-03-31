package com.hrpilot.backend.employee;

import com.hrpilot.backend.audit.AuditLogService;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.common.storage.FileStorageService;
import com.hrpilot.backend.department.DepartmentRepository;
import com.hrpilot.backend.department.DepartmentScopeService;
import com.hrpilot.backend.employee.dto.CreateEmployeeRequest;
import com.hrpilot.backend.employee.dto.EmployeeResponse;
import com.hrpilot.backend.leave.LeaveBalanceRepository;
import com.hrpilot.backend.leave.LeaveRequestHistoryRepository;
import com.hrpilot.backend.leave.LeaveRequestRepository;
import com.hrpilot.backend.notification.NotificationService;
import com.hrpilot.backend.payroll.PayrollComponentRepository;
import com.hrpilot.backend.payroll.PayrollRepository;
import com.hrpilot.backend.user.CurrentUserService;
import com.hrpilot.backend.user.Role;
import com.hrpilot.backend.user.User;
import com.hrpilot.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentScopeService departmentScopeService;

    @Mock
    private EmploymentHistoryRepository historyRepository;

    @Mock
    private EmployeeDocumentRepository employeeDocumentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private LeaveRequestHistoryRepository leaveRequestHistoryRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private PayrollComponentRepository payrollComponentRepository;

    @Mock
    private PayrollRepository payrollRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private User actorUser;

    @BeforeEach
    void setUp() {
        actorUser = User.builder()
            .id(5L)
            .email("hr@test.com")
            .role(Role.HR_MANAGER)
            .isActive(true)
            .build();
        lenient().when(currentUserService.getCurrentUserEntity()).thenReturn(actorUser);
    }

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
        User user = buildUser();
        CreateEmployeeRequest request = new CreateEmployeeRequest(
            1L, "John", "Doe", "Developer",
            new BigDecimal("5000"), LocalDate.of(2024, 1, 15), null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.existsByUserId(1L)).thenReturn(false);
        Employee savedEmployee = buildEmployee(user);
        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);

        EmployeeResponse response = employeeService.createEmployee(request);

        assertNotNull(response);
        assertEquals("John", response.firstName());
        assertEquals("Doe", response.lastName());
        assertEquals("emp@test.com", response.email());
    }

    @Test
    void createEmployee_userNotFound_throwsException() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
            99L, "John", "Doe", "Developer",
            new BigDecimal("5000"), LocalDate.of(2024, 1, 15), null);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.createEmployee(request));
    }

    @Test
    void getAllEmployees_returnsAllEmployees() {
        User user = buildUser();
        Employee employee = buildEmployee(user);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Employee> page = new PageImpl<>(List.of(employee), pageable, 1);
        when(employeeRepository.findAll(argThat((org.springframework.data.jpa.domain.Specification<Employee> spec) -> true), org.mockito.ArgumentMatchers.eq(pageable))).thenReturn(page);

        Page<EmployeeResponse> responses = employeeService.getAllEmployees(pageable);

        assertEquals(1, responses.getTotalElements());
        assertEquals("John", responses.getContent().get(0).firstName());
    }

    @Test
    void getEmployeeById_employeeExists_returnsResponse() {
        User user = buildUser();
        Employee employee = buildEmployee(user);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        EmployeeResponse response = employeeService.getEmployeeById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("John", response.firstName());
    }

    @Test
    void getEmployeeById_employeeNotFound_throwsException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.getEmployeeById(99L));
    }

    @Test
    void deleteEmployee_employeeExists_deletesSuccessfully() {
        User user = buildUser();
        Employee employee = buildEmployee(user);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeDocumentRepository.findByEmployeeIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        employeeService.deleteEmployee(1L);

        assertTrue(true);
    }

    @Test
    void deleteEmployee_employeeNotFound_throwsException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.deleteEmployee(99L));
    }
}
