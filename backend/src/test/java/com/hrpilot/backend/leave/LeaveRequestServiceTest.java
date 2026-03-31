package com.hrpilot.backend.leave;

import com.hrpilot.backend.audit.AuditLogService;
import com.hrpilot.backend.common.exception.BusinessRuleException;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.department.DepartmentScopeService;
import com.hrpilot.backend.employee.Employee;
import com.hrpilot.backend.employee.EmployeeRepository;
import com.hrpilot.backend.leave.dto.CreateLeaveRequest;
import com.hrpilot.backend.leave.dto.LeaveRequestResponse;
import com.hrpilot.backend.notification.NotificationService;
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
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private LeaveRequestHistoryRepository leaveRequestHistoryRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentScopeService departmentScopeService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LeaveBalanceService leaveBalanceService;

    @Mock
    private LeaveCalendarService leaveCalendarService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private LeaveRequestService leaveRequestService;

    private User actorUser;
    private Employee employee;

    @BeforeEach
    void setUp() {
        actorUser = User.builder()
            .id(10L)
            .email("admin@hrpilot.com")
            .role(Role.ADMIN)
            .isActive(true)
            .build();

        employee = Employee.builder()
            .id(1L)
            .user(User.builder().id(1L).email("john@hrpilot.com").role(Role.EMPLOYEE).build())
            .firstName("John")
            .lastName("Doe")
            .build();

        when(currentUserService.getCurrentUserEntity()).thenReturn(actorUser);
        lenient().when(userRepository.findByRoleIn(any())).thenReturn(List.of());
    }

    private LeaveRequest buildLeaveRequest(LeaveStatus status) {
        return LeaveRequest.builder()
            .id(1L)
            .employee(employee)
            .type(LeaveType.ANNUAL)
            .startDate(LocalDate.of(2026, 4, 1))
            .endDate(LocalDate.of(2026, 4, 5))
            .workingDays(3)
            .status(status)
            .reason("Vacation")
            .build();
    }

    @Test
    void createLeaveRequest_success() {
        CreateLeaveRequest request = new CreateLeaveRequest(
            1L,
            LeaveType.ANNUAL,
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 4, 5),
            "Vacation"
        );
        LeaveRequest savedLeave = buildLeaveRequest(LeaveStatus.PENDING);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.existsByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            eq(1L), any(), any(), any()
        )).thenReturn(false);
        when(leaveCalendarService.calculateWorkingDays(request.startDate(), request.endDate())).thenReturn(3);
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(savedLeave);

        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(LeaveType.ANNUAL, response.type());
        assertEquals(LeaveStatus.PENDING, response.status());
        assertEquals(3, response.workingDays());
    }

    @Test
    void createLeaveRequest_employeeNotFound_throwsException() {
        CreateLeaveRequest request = new CreateLeaveRequest(
            99L,
            LeaveType.ANNUAL,
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 4, 5),
            "Vacation"
        );
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leaveRequestService.createLeaveRequest(request));
    }

    @Test
    void getVisibleLeaveRequests_returnsAll() {
        LeaveRequest leave = buildLeaveRequest(LeaveStatus.PENDING);
        Pageable pageable = PageRequest.of(0, 10);
        Page<LeaveRequest> page = new PageImpl<>(List.of(leave), pageable, 1);
        when(leaveRequestRepository.findAll(org.mockito.ArgumentMatchers.<Specification<LeaveRequest>>any(), eq(pageable))).thenReturn(page);

        Page<LeaveRequestResponse> responses = leaveRequestService.getVisibleLeaveRequests(null, null, null, null, pageable);

        assertEquals(1, responses.getTotalElements());
        assertEquals(LeaveType.ANNUAL, responses.getContent().get(0).type());
    }

    @Test
    void getLeaveRequestsByEmployee_success() {
        LeaveRequest leave = buildLeaveRequest(LeaveStatus.PENDING);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        Pageable pageable = PageRequest.of(0, 10);
        when(leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(new PageImpl<>(List.of(leave)));

        Page<LeaveRequestResponse> responses = leaveRequestService.getLeaveRequestsByEmployee(1L, pageable);

        assertEquals(1, responses.getTotalElements());
        assertEquals(1L, responses.getContent().get(0).employeeId());
    }

    @Test
    void approveLeaveRequest_success() {
        LeaveRequest leave = buildLeaveRequest(LeaveStatus.PENDING);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(leave);

        LeaveRequestResponse response = leaveRequestService.approveLeaveRequest(1L);

        assertNotNull(response);
        verify(leaveBalanceService).deductBalance(1L, LeaveType.ANNUAL, 2026, 3);
        assertEquals(LeaveStatus.APPROVED, response.status());
    }

    @Test
    void approveLeaveRequest_notFound_throwsException() {
        when(leaveRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leaveRequestService.approveLeaveRequest(99L));
    }

    @Test
    void rejectLeaveRequest_success() {
        LeaveRequest leave = buildLeaveRequest(LeaveStatus.PENDING);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(leave);

        LeaveRequestResponse response = leaveRequestService.rejectLeaveRequest(1L, "Not enough coverage");

        assertNotNull(response);
        assertEquals(LeaveStatus.REJECTED, response.status());
        assertEquals("Not enough coverage", response.rejectionReason());
    }

    @Test
    void rejectLeaveRequest_notFound_throwsException() {
        when(leaveRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leaveRequestService.rejectLeaveRequest(99L, "Nope"));
    }

    @Test
    void createLeaveRequest_startDateAfterEndDate_throwsException() {
        CreateLeaveRequest request = new CreateLeaveRequest(
            1L,
            LeaveType.ANNUAL,
            LocalDate.of(2026, 4, 10),
            LocalDate.of(2026, 4, 5),
            "Vacation"
        );
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        assertThrows(BusinessRuleException.class, () -> leaveRequestService.createLeaveRequest(request));
    }

    @Test
    void approveLeaveRequest_alreadyApproved_throwsException() {
        LeaveRequest leave = buildLeaveRequest(LeaveStatus.APPROVED);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leave));

        assertThrows(BusinessRuleException.class, () -> leaveRequestService.approveLeaveRequest(1L));
    }

    @Test
    void rejectLeaveRequest_alreadyRejected_throwsException() {
        LeaveRequest leave = buildLeaveRequest(LeaveStatus.REJECTED);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leave));

        assertThrows(BusinessRuleException.class, () -> leaveRequestService.rejectLeaveRequest(1L, "Nope"));
    }
}
