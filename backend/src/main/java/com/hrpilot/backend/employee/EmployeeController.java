package com.hrpilot.backend.employee;

import com.hrpilot.backend.employee.dto.CreateEmployeeRequest;
import com.hrpilot.backend.employee.dto.EmployeeDetailResponse;
import com.hrpilot.backend.employee.dto.EmployeeDocumentResponse;
import com.hrpilot.backend.employee.dto.UpdateEmployeeRequest;
import com.hrpilot.backend.employee.dto.EmployeeResponse;
import com.hrpilot.backend.employee.dto.EmploymentHistoryResponse;
import com.hrpilot.backend.common.storage.StoredFileContent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@Tag(name = "Employees", description = "Employee management operations")
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;

    @Operation(summary = "Create a new employee")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeResponse employeeResponse = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeResponse);
    }

    @Operation(summary = "List employees with optional filtering")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
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

    @Operation(summary = "Export employees to CSV")
    @GetMapping("/export/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<byte[]> exportCsv() {
        String csv = employeeService.exportToCsv();
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=employees.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv.getBytes());
    }

    @Operation(summary = "Get employee by ID")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @Operation(summary = "Get detailed employee information")
    @GetMapping("/{id}/detail")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeDetailResponse> getEmployeeDetail(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeDetail(id));
    }

    @Operation(summary = "Update an employee")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<EmployeeResponse> updateEmployee(@PathVariable Long id,
                                                            @Valid @RequestBody UpdateEmployeeRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @Operation(summary = "Get employment history")
    @GetMapping("/{id}/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EmploymentHistoryResponse>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmploymentHistory(id));
    }

    @Operation(summary = "Delete an employee")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upload employee photo")
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeResponse> uploadPhoto(@PathVariable Long id,
                                                         @RequestParam("file") MultipartFile file) {
        EmployeeResponse response = employeeService.uploadPhoto(id, file);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Download employee photo")
    @GetMapping("/{id}/photo/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InputStreamResource> downloadPhoto(@PathVariable Long id) {
        StoredFileContent fileContent = employeeService.downloadPhoto(id);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(fileContent.contentType()))
            .contentLength(fileContent.contentLength())
            .body(fileContent.resource());
    }

    @Operation(summary = "List employee documents")
    @GetMapping("/{id}/documents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EmployeeDocumentResponse>> getDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeDocuments(id));
    }

    @Operation(summary = "Upload employee document")
    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<EmployeeDocumentResponse> uploadDocument(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(employeeService.uploadDocument(id, title, description, file));
    }

    @Operation(summary = "Download employee document")
    @GetMapping("/{id}/documents/{documentId}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InputStreamResource> downloadDocument(
            @PathVariable Long id,
            @PathVariable Long documentId) {
        StoredFileContent fileContent = employeeService.downloadDocument(id, documentId);
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"" + fileContent.filename() + "\"")
            .contentType(MediaType.parseMediaType(fileContent.contentType()))
            .contentLength(fileContent.contentLength())
            .body(fileContent.resource());
    }
}
