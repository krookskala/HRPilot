package com.hrpilot.backend.leave;

import com.hrpilot.backend.leave.dto.LeaveBalanceResponse;
import com.hrpilot.backend.leave.dto.LeaveRequestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "My Leave", description = "Current user leave requests and balances")
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MyLeaveController {

    private final LeaveRequestService leaveRequestService;
    private final LeaveBalanceService leaveBalanceService;

    @Operation(summary = "Get current user's leave requests")
    @GetMapping("/leave-requests")
    public ResponseEntity<Page<LeaveRequestResponse>> getMyLeaveRequests(Pageable pageable) {
        return ResponseEntity.ok(leaveRequestService.getCurrentUserLeaveRequests(pageable));
    }

    @Operation(summary = "Get current user's leave balances")
    @GetMapping("/leave-balances")
    public ResponseEntity<List<LeaveBalanceResponse>> getMyBalances(@RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(leaveBalanceService.getCurrentUserBalances(targetYear));
    }
}
