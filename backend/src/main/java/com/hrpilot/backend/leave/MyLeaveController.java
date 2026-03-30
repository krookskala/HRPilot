package com.hrpilot.backend.leave;

import com.hrpilot.backend.leave.dto.LeaveBalanceResponse;
import com.hrpilot.backend.leave.dto.LeaveRequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MyLeaveController {

    private final LeaveRequestService leaveRequestService;
    private final LeaveBalanceService leaveBalanceService;

    @GetMapping("/leave-requests")
    public ResponseEntity<List<LeaveRequestResponse>> getMyLeaveRequests() {
        return ResponseEntity.ok(leaveRequestService.getCurrentUserLeaveRequests());
    }

    @GetMapping("/leave-balances")
    public ResponseEntity<List<LeaveBalanceResponse>> getMyBalances(@RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(leaveBalanceService.getCurrentUserBalances(targetYear));
    }
}
