package com.hrpilot.backend.leave;

import com.hrpilot.backend.leave.dto.CreateLeaveRequest;
import com.hrpilot.backend.leave.dto.LeaveActionRequest;
import com.hrpilot.backend.leave.dto.LeaveBalanceResponse;
import com.hrpilot.backend.leave.dto.LeaveRequestHistoryResponse;
import com.hrpilot.backend.leave.dto.LeaveRequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/leave-requests")
@RequiredArgsConstructor
public class LeaveRequestController {
    private final LeaveRequestService leaveRequestService;
    private final LeaveBalanceService leaveBalanceService;

    @PostMapping
    public ResponseEntity<LeaveRequestResponse> create(@Valid @RequestBody
CreateLeaveRequest request) {
        return ResponseEntity.status(201).body(leaveRequestService.createLeaveRequest(request));
    }

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

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LeaveRequestResponse>>
getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveRequestService.getLeaveRequestsByEmployee(employeeId));
    }

    @GetMapping("/balances/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'DEPARTMENT_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<LeaveBalanceResponse>> getBalances(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(leaveBalanceService.getBalancesForEmployee(employeeId, targetYear));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<LeaveRequestHistoryResponse>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(leaveRequestService.getLeaveRequestHistory(id));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'DEPARTMENT_MANAGER')")
    public ResponseEntity<LeaveRequestResponse> approve(@PathVariable Long id) {
        return
ResponseEntity.ok(leaveRequestService.approveLeaveRequest(id));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'DEPARTMENT_MANAGER')")
    public ResponseEntity<LeaveRequestResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody LeaveActionRequest request) {
        return
ResponseEntity.ok(leaveRequestService.rejectLeaveRequest(id, request.reason()));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<LeaveRequestResponse> cancel(
            @PathVariable Long id,
            @Valid @RequestBody LeaveActionRequest request) {
        return ResponseEntity.ok(leaveRequestService.cancelLeaveRequest(id, request.reason()));
    }
}
