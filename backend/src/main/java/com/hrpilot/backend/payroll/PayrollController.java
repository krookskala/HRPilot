package com.hrpilot.backend.payroll;

import com.hrpilot.backend.payroll.dto.CreatePayrollRequest;
import com.hrpilot.backend.payroll.dto.CreatePayrollRunRequest;
import com.hrpilot.backend.payroll.dto.PayrollComponentResponse;
import com.hrpilot.backend.payroll.dto.PayrollPreviewRequest;
import com.hrpilot.backend.payroll.dto.PayrollResponse;
import com.hrpilot.backend.payroll.dto.PayrollRunResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

@Tag(name = "Payrolls", description = "Payroll management and payslip operations")
@RestController
@RequestMapping("/api/payrolls")
@RequiredArgsConstructor
public class PayrollController {
    private final PayrollService payrollService;

    @Operation(summary = "Create a payroll record")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<PayrollResponse> create(@Valid @RequestBody
CreatePayrollRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payrollService.createPayroll(request));
    }

    @Operation(summary = "Preview payroll calculation")
    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<PayrollResponse> preview(@Valid @RequestBody PayrollPreviewRequest request) {
        return ResponseEntity.ok(payrollService.previewPayroll(request));
    }

    @Operation(summary = "Create a payroll run")
    @PostMapping("/runs")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<PayrollRunResponse> createRun(@Valid @RequestBody CreatePayrollRunRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payrollService.createPayrollRun(request));
    }

    @Operation(summary = "List all payroll records")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Page<PayrollResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(payrollService.getAllPayrolls(pageable));
    }

    @Operation(summary = "List payroll runs")
    @GetMapping("/runs")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Page<PayrollRunResponse>> getRuns(Pageable pageable) {
        return ResponseEntity.ok(payrollService.getPayrollRuns(pageable));
    }

    @Operation(summary = "Get payrolls by employee")
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PayrollResponse>> getByEmployee(@PathVariable Long employeeId, Pageable pageable) {
        return ResponseEntity.ok(payrollService.getPayrollsByEmployee(employeeId, pageable));
    }

    @Operation(summary = "Get payroll components")
    @GetMapping("/{id}/components")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PayrollComponentResponse>> getComponents(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.getComponents(id));
    }

    @Operation(summary = "Download payslip as PDF")
    @GetMapping("/{id}/payslip")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InputStreamResource> downloadPayslip(@PathVariable Long id) {
        var file = payrollService.downloadPayslip(id);
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"" + file.filename() + "\"")
            .contentType(MediaType.parseMediaType(file.contentType()))
            .contentLength(file.contentLength())
            .body(file.resource());
    }

    @Operation(summary = "Mark payroll as paid")
    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<PayrollResponse> markAsPaid(@PathVariable Long id) {
        return
ResponseEntity.ok(payrollService.markAsPaid(id));
    }

    @Operation(summary = "Publish a payroll run")
    @PutMapping("/runs/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<PayrollRunResponse> publishRun(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.publishRun(id));
    }

    @Operation(summary = "Mark a payroll run as paid")
    @PutMapping("/runs/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<PayrollRunResponse> payRun(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.payRun(id));
    }
}
