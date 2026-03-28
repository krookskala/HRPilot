package com.hrpilot.backend.payroll;

import com.hrpilot.backend.payroll.dto.CreatePayrollRequest;
import com.hrpilot.backend.payroll.dto.PayrollResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/payrolls")
@RequiredArgsConstructor
public class PayrollController {
    private final PayrollService payrollService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<PayrollResponse> create(@Valid @RequestBody
CreatePayrollRequest request) {
        return ResponseEntity.status(201).body(payrollService.createPayroll(request));
    }

    @GetMapping
    public ResponseEntity<Page<PayrollResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(payrollService.getAllPayrolls(pageable));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<PayrollResponse>>
getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(payrollService.getPayrollsByEmployee(employeeId));
    }

    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<PayrollResponse> markAsPaid(@PathVariable Long id) {
        return
ResponseEntity.ok(payrollService.markAsPaid(id));
    }
}