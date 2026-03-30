package com.hrpilot.backend.payroll;

import com.hrpilot.backend.payroll.dto.CreatePayrollRequest;
import com.hrpilot.backend.payroll.dto.CreatePayrollRunRequest;
import com.hrpilot.backend.payroll.dto.PayrollComponentResponse;
import com.hrpilot.backend.payroll.dto.PayrollPreviewRequest;
import com.hrpilot.backend.payroll.dto.PayrollResponse;
import com.hrpilot.backend.payroll.dto.PayrollRunResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
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

    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<PayrollResponse> preview(@Valid @RequestBody PayrollPreviewRequest request) {
        return ResponseEntity.ok(payrollService.previewPayroll(request));
    }

    @PostMapping("/runs")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<PayrollRunResponse> createRun(@Valid @RequestBody CreatePayrollRunRequest request) {
        return ResponseEntity.status(201).body(payrollService.createPayrollRun(request));
    }

    @GetMapping
    public ResponseEntity<Page<PayrollResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(payrollService.getAllPayrolls(pageable));
    }

    @GetMapping("/runs")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Page<PayrollRunResponse>> getRuns(Pageable pageable) {
        return ResponseEntity.ok(payrollService.getPayrollRuns(pageable));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<PayrollResponse>>
getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(payrollService.getPayrollsByEmployee(employeeId));
    }

    @GetMapping("/{id}/components")
    public ResponseEntity<List<PayrollComponentResponse>> getComponents(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.getComponents(id));
    }

    @GetMapping("/{id}/payslip")
    public ResponseEntity<InputStreamResource> downloadPayslip(@PathVariable Long id) {
        var file = payrollService.downloadPayslip(id);
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"" + file.filename() + "\"")
            .contentType(MediaType.parseMediaType(file.contentType()))
            .contentLength(file.contentLength())
            .body(file.resource());
    }

    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<PayrollResponse> markAsPaid(@PathVariable Long id) {
        return
ResponseEntity.ok(payrollService.markAsPaid(id));
    }

    @PutMapping("/runs/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<PayrollRunResponse> publishRun(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.publishRun(id));
    }

    @PutMapping("/runs/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<PayrollRunResponse> payRun(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.payRun(id));
    }
}
