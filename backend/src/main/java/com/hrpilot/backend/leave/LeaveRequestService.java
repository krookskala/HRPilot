package com.hrpilot.backend.leave;

import com.hrpilot.backend.employee.Employee;
import com.hrpilot.backend.employee.EmployeeRepository;
import com.hrpilot.backend.leave.dto.CreateLeaveRequest;
import com.hrpilot.backend.leave.dto.LeaveRequestResponse;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.common.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveRequestService {
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceService leaveBalanceService;

    public LeaveRequestResponse createLeaveRequest(CreateLeaveRequest request) {
        log.info("Creating leave request for employee id: {}", request.employeeId());
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.employeeId()));

        if (request.startDate().isAfter(request.endDate())) {
            throw new BusinessRuleException("Start date cannot be after end date");
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employee(employee)
                .type(request.type())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .reason(request.reason())
                .build();

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request created successfully with id: {}", saved.getId());
        return toResponse(saved);
    }

    public Page<LeaveRequestResponse> getAllLeaveRequests(Pageable pageable) {
        return leaveRequestRepository.findAll(pageable)
                .map(this::toResponse);
    }

    public List<LeaveRequestResponse> getLeaveRequestsByEmployee(Long employeeId) {
        return leaveRequestRepository.findByEmployeeId(employeeId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LeaveRequestResponse approveLeaveRequest(Long id) {
        log.info("Approving leave request with id: {}", id);
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", id));
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessRuleException("Only PENDING leave requests can be approved");
        }

        leaveBalanceService.deductBalance(
            leaveRequest.getEmployee().getId(),
            leaveRequest.getType(),
            leaveRequest.getStartDate(),
            leaveRequest.getEndDate()
        );

        leaveRequest.setStatus(LeaveStatus.APPROVED);
        return toResponse(leaveRequestRepository.save(leaveRequest));
    }

    @Transactional
    public LeaveRequestResponse rejectLeaveRequest(Long id) {
        log.info("Rejecting leave request with id: {}", id);
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", id));
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessRuleException("Only PENDING leave requests can be rejected");
        }
        leaveRequest.setStatus(LeaveStatus.REJECTED);
        return toResponse(leaveRequestRepository.save(leaveRequest));
    }

    private LeaveRequestResponse toResponse(LeaveRequest lr) {
        return new LeaveRequestResponse(
                lr.getId(),
                lr.getEmployee().getId(),
                lr.getEmployee().getFirstName() + " " + lr.getEmployee().getLastName(),
                lr.getType(),
                lr.getStartDate(),
                lr.getEndDate(),
                lr.getStatus(),
                lr.getReason()
        );
    }
}
