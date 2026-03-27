package com.hrpilot.backend.leave;

import com.hrpilot.backend.leave.dto.CreateLeaveRequest;
import com.hrpilot.backend.leave.dto.LeaveRequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/leave-requests")
@RequiredArgsConstructor
public class LeaveRequestController {
    private final LeaveRequestService leaveRequestService;

    @PostMapping
    public ResponseEntity<LeaveRequestResponse> create(@Valid @RequestBody
CreateLeaveRequest request) {
        return ResponseEntity.status(201).body(leaveRequestService.createLeaveRequest(request));
    }

    @GetMapping
    public ResponseEntity<List<LeaveRequestResponse>> getAll() {
        return
ResponseEntity.ok(leaveRequestService.getAllLeaveRequests());
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LeaveRequestResponse>>
getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveRequestService.getLeaveRequestsByEmployee(employeeId));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<LeaveRequestResponse> approve(@PathVariable Long id) {
        return
ResponseEntity.ok(leaveRequestService.approveLeaveRequest(id));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<LeaveRequestResponse> reject(@PathVariable Long id) {
        return
ResponseEntity.ok(leaveRequestService.rejectLeaveRequest(id));
    }
}