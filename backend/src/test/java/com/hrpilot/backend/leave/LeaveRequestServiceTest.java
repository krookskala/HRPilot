package com.hrpilot.backend.leave;

import com.hrpilot.backend.employee.Employee;
import com.hrpilot.backend.employee.EmployeeRepository;
import com.hrpilot.backend.leave.dto.CreateLeaveRequest;
import com.hrpilot.backend.leave.dto.LeaveRequestResponse;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.common.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private LeaveRequestService leaveRequestService;

    private Employee buildEmployee() {
        return Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    private LeaveRequest buildLeaveRequest(Employee employee, LeaveStatus status) {
        return LeaveRequest.builder()
                .id(1L)
                .employee(employee)
                .type(LeaveType.ANNUAL)
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 4, 5))
                .status(status)
                .reason("Vacation")
                .build();
    }

    @Test
    void createLeaveRequest_success() {
        // Arrange
        Employee employee = buildEmployee();
        CreateLeaveRequest request = new CreateLeaveRequest(
                1L,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 5),
                "Vacation"
        );
        LeaveRequest savedLeave = buildLeaveRequest(employee, LeaveStatus.PENDING);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(savedLeave);

        // Act
        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(LeaveType.ANNUAL, response.type());
        assertEquals(LeaveStatus.PENDING, response.status());
        assertEquals("John Doe", response.employeeFullName());
    }

    @Test
    void createLeaveRequest_employeeNotFound_throwsException() {
        // Arrange
        CreateLeaveRequest request = new CreateLeaveRequest(
                99L,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 5),
                "Vacation"
        );
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> leaveRequestService.createLeaveRequest(request));
    }

    @Test
    void getAllLeaveRequests_returnsAll() {
        // Arrange
        Employee employee = buildEmployee();
        LeaveRequest leave = buildLeaveRequest(employee, LeaveStatus.PENDING);
        Pageable pageable = PageRequest.of(0, 10);
        Page<LeaveRequest> page = new PageImpl<>(List.of(leave), pageable, 1);
        when(leaveRequestRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<LeaveRequestResponse> responses = leaveRequestService.getAllLeaveRequests(pageable);

        // Assert
        assertEquals(1, responses.getTotalElements());
        assertEquals(LeaveType.ANNUAL, responses.getContent().get(0).type());
    }

    @Test
    void getLeaveRequestsByEmployee_success() {
        // Arrange
        Employee employee = buildEmployee();
        LeaveRequest leave = buildLeaveRequest(employee, LeaveStatus.PENDING);
        when(leaveRequestRepository.findByEmployeeId(1L)).thenReturn(List.of(leave));

        // Act
        List<LeaveRequestResponse> responses = leaveRequestService.getLeaveRequestsByEmployee(1L);

        // Assert
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).employeeId());
    }

    @Test
    void approveLeaveRequest_success() {
        // Arrange
        Employee employee = buildEmployee();
        LeaveRequest leave = buildLeaveRequest(employee, LeaveStatus.PENDING);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(leave);

        // Act
        LeaveRequestResponse response = leaveRequestService.approveLeaveRequest(1L);

        // Assert
        assertNotNull(response);
        assertEquals(LeaveStatus.APPROVED, response.status());
    }

    @Test
    void approveLeaveRequest_notFound_throwsException() {
        // Arrange
        when(leaveRequestRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> leaveRequestService.approveLeaveRequest(99L));
    }

    @Test
    void rejectLeaveRequest_success() {
        // Arrange
        Employee employee = buildEmployee();
        LeaveRequest leave = buildLeaveRequest(employee, LeaveStatus.PENDING);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(leave);

        // Act
        LeaveRequestResponse response = leaveRequestService.rejectLeaveRequest(1L);

        // Assert
        assertNotNull(response);
        assertEquals(LeaveStatus.REJECTED, response.status());
    }

    @Test
    void rejectLeaveRequest_notFound_throwsException() {
        // Arrange
        when(leaveRequestRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> leaveRequestService.rejectLeaveRequest(99L));
    }

    @Test
    void createLeaveRequest_startDateAfterEndDate_throwsException() {
        // Arrange
        Employee employee = buildEmployee();
        CreateLeaveRequest request = new CreateLeaveRequest(
                1L,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 5),
                "Vacation"
        );
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> leaveRequestService.createLeaveRequest(request));
    }

    @Test
    void approveLeaveRequest_alreadyApproved_throwsException() {
        // Arrange
        Employee employee = buildEmployee();
        LeaveRequest leave = buildLeaveRequest(employee, LeaveStatus.APPROVED);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leave));

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> leaveRequestService.approveLeaveRequest(1L));
    }

    @Test
    void rejectLeaveRequest_alreadyRejected_throwsException() {
        // Arrange
        Employee employee = buildEmployee();
        LeaveRequest leave = buildLeaveRequest(employee, LeaveStatus.REJECTED);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leave));

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> leaveRequestService.rejectLeaveRequest(1L));
    }
}
