package com.hrpilot.backend.leave;

import com.hrpilot.backend.leave.dto.CreateLeaveRequest;
import com.hrpilot.backend.leave.dto.LeaveActionRequest;
import com.hrpilot.backend.leave.dto.LeaveBalanceResponse;
import com.hrpilot.backend.leave.dto.LeaveRequestHistoryResponse;
import com.hrpilot.backend.leave.dto.LeaveRequestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

@Tag(name = "Leave Requests", description = "Leave request management operations")
@RestController
@RequestMapping("/api/leave-requests")
@RequiredArgsConstructor
public class LeaveRequestController {
    private final LeaveRequestService leaveRequestService;
    private final LeaveBalanceService leaveBalanceService;

    @Operation(summary = "Create a new leave request")
    @PostMapping
    public ResponseEntity<LeaveRequestResponse> create(@Valid @RequestBody
CreateLeaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveRequestService.createLeaveRequest(request));
    }

    @Operation(summary = "List leave requests with optional filtering")
    @GetMapping
    public ResponseEntity<Page<LeaveRequestResponse>> getAll(
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) LeaveType type,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long departmentId,
            Pageable pageable) {
        return ResponseEntity.ok(
            leaveRequestService.getVisibleLeaveRequests(status, type, employeeId, departmentId, pageable)
        );
    }

    @Operation(summary = "Get leave requests by employee")
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Page<LeaveRequestResponse>> getByEmployee(@PathVariable Long employeeId, Pageable pageable) {
        return ResponseEntity.ok(leaveRequestService.getLeaveRequestsByEmployee(employeeId, pageable));
    }

    @Operation(summary = "Get leave balances for an employee")
    @GetMapping("/balances/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'DEPARTMENT_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<LeaveBalanceResponse>> getBalances(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(leaveBalanceService.getBalancesForEmployee(employeeId, targetYear));
    }

    @Operation(summary = "Get leave request history")
    @GetMapping("/{id}/history")
    public ResponseEntity<List<LeaveRequestHistoryResponse>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(leaveRequestService.getLeaveRequestHistory(id));
    }

    @Operation(summary = "Approve a leave request")
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'DEPARTMENT_MANAGER')")
    public ResponseEntity<LeaveRequestResponse> approve(@PathVariable Long id) {
        return
ResponseEntity.ok(leaveRequestService.approveLeaveRequest(id));
    }

    @Operation(summary = "Reject a leave request")
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'DEPARTMENT_MANAGER')")
    public ResponseEntity<LeaveRequestResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody LeaveActionRequest request) {
        return
ResponseEntity.ok(leaveRequestService.rejectLeaveRequest(id, request.reason()));
    }

    @Operation(summary = "Cancel a leave request")
    @PutMapping("/{id}/cancel")
    public ResponseEntity<LeaveRequestResponse> cancel(
            @PathVariable Long id,
            @Valid @RequestBody LeaveActionRequest request) {
        return ResponseEntity.ok(leaveRequestService.cancelLeaveRequest(id, request.reason()));
    }
}
