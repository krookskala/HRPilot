package com.hrpilot.backend.payroll;

import com.hrpilot.backend.payroll.dto.PayrollResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Tag(name = "My Payroll", description = "Current user payroll and payslip operations")
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MyPayrollController {

    private final PayrollService payrollService;

    @Operation(summary = "Get current user's payrolls")
    @GetMapping("/payrolls")
    public ResponseEntity<Page<PayrollResponse>> getMyPayrolls(Pageable pageable) {
        return ResponseEntity.ok(payrollService.getCurrentUserPayrolls(pageable));
    }

    @Operation(summary = "Download current user's payslip")
    @GetMapping("/payrolls/{id}/payslip")
    public ResponseEntity<InputStreamResource> downloadMyPayslip(@PathVariable Long id) {
        var file = payrollService.downloadPayslipForCurrentUser(id);
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"" + file.filename() + "\"")
            .contentType(MediaType.parseMediaType(file.contentType()))
            .contentLength(file.contentLength())
            .body(file.resource());
    }
}
