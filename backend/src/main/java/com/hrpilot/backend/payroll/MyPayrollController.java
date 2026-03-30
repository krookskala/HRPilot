package com.hrpilot.backend.payroll;

import com.hrpilot.backend.payroll.dto.PayrollResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MyPayrollController {

    private final PayrollService payrollService;

    @GetMapping("/payrolls")
    public ResponseEntity<List<PayrollResponse>> getMyPayrolls() {
        return ResponseEntity.ok(payrollService.getCurrentUserPayrolls());
    }

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
