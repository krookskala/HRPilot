package com.hrpilot.backend.leave;

import com.hrpilot.backend.employee.Employee;
import com.hrpilot.backend.employee.EmployeeRepository;
import com.hrpilot.backend.leave.dto.CreateLeaveRequest;
import com.hrpilot.backend.leave.dto.LeaveRequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;

    public LeaveRequestResponse createLeaveRequest(CreateLeaveRequest request) {
        Employee employee = 
    employeeRepository.findById(request.employeeId())
                    .orElseThrow(() -> new RuntimeException("Employee Not Found"));
        
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employee(employee)
                .type(request.type())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .reason(request.reason())
                .build();

        return toResponse(leaveRequestRepository.save(leaveRequest));
    }

    public List<LeaveRequestResponse> getAllLeaveRequests() {
        return leaveRequestRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<LeaveRequestResponse> getLeaveRequestsByEmployee(Long
    employeeId) {
        return
    leaveRequestRepository.findByEmployeeId(employeeId).stream()
                    .map(this::toResponse)
                    .toList();
    }

    public LeaveRequestResponse approveLeaveRequest(Long id) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave Request Not Found"));
        leaveRequest.setStatus(LeaveStatus.APPROVED);
        return toResponse(leaveRequestRepository.save(leaveRequest));
    }

    public LeaveRequestResponse rejectLeaveRequest(Long id) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave Request Not Found"));
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