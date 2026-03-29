package com.hrpilot.backend.employee;

import com.hrpilot.backend.employee.dto.CreateEmployeeRequest;
import com.hrpilot.backend.employee.dto.UpdateEmployeeRequest;
import com.hrpilot.backend.employee.dto.EmployeeResponse;
import com.hrpilot.backend.employee.dto.EmploymentHistoryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeResponse employeeResponse = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeResponse);
    }

    @GetMapping
    public ResponseEntity<Page<EmployeeResponse>> getAllEmployees(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String position,
            Pageable pageable) {
        boolean hasFilters = (search != null && !search.isBlank())
                || departmentId != null
                || (position != null && !position.isBlank());

        Page<EmployeeResponse> result = hasFilters
                ? employeeService.searchEmployees(search, departmentId, position, pageable)
                : employeeService.getAllEmployees(pageable);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<byte[]> exportCsv() {
        String csv = employeeService.exportToCsv();
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=employees.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv.getBytes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<EmployeeResponse> updateEmployee(@PathVariable Long id,
                                                            @RequestBody UpdateEmployeeRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<EmploymentHistoryResponse>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmploymentHistory(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<EmployeeResponse> uploadPhoto(@PathVariable Long id,
                                                         @RequestParam("file") MultipartFile file) {
        EmployeeResponse response = employeeService.uploadPhoto(id, file);
        return ResponseEntity.ok(response);
    }
}
