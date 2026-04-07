package com.hrpilot.backend.employee;

import com.hrpilot.backend.audit.AuditLogService;
import com.hrpilot.backend.common.exception.BusinessRuleException;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.common.storage.FileStorageService;
import com.hrpilot.backend.common.storage.StoredFileContent;
import com.hrpilot.backend.common.storage.StoredFileMetadata;
import com.hrpilot.backend.department.Department;
import com.hrpilot.backend.department.DepartmentRepository;
import com.hrpilot.backend.department.DepartmentScopeService;
import com.hrpilot.backend.employee.dto.CreateEmployeeRequest;
import com.hrpilot.backend.employee.dto.EmployeeDetailResponse;
import com.hrpilot.backend.employee.dto.EmployeeDocumentResponse;
import com.hrpilot.backend.employee.dto.EmployeeResponse;
import com.hrpilot.backend.employee.dto.EmploymentHistoryResponse;
import com.hrpilot.backend.employee.dto.UpdateEmployeeRequest;
import com.hrpilot.backend.leave.LeaveBalanceRepository;
import com.hrpilot.backend.leave.LeaveRequestHistoryRepository;
import com.hrpilot.backend.leave.LeaveRequestRepository;
import com.hrpilot.backend.notification.NotificationService;
import com.hrpilot.backend.notification.NotificationType;
import com.hrpilot.backend.payroll.PayrollComponentRepository;
import com.hrpilot.backend.payroll.PayrollRepository;
import com.hrpilot.backend.user.CurrentUserService;
import com.hrpilot.backend.user.Role;
import com.hrpilot.backend.user.User;
import com.hrpilot.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.core.io.InputStreamResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "application/pdf",
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final DepartmentScopeService departmentScopeService;
    private final EmploymentHistoryRepository historyRepository;
    private final EmployeeDocumentRepository employeeDocumentRepository;
    private final FileStorageService fileStorageService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final LeaveRequestHistoryRepository leaveRequestHistoryRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final PayrollComponentRepository payrollComponentRepository;
    private final PayrollRepository payrollRepository;

    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        log.info("Creating employee for user id: {}", request.userId());
        User user = userRepository.findById(request.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.userId()));

        if (employeeRepository.existsByUserId(request.userId())) {
            throw new BusinessRuleException("This user is already linked to an employee record");
        }

        Department department = null;
        if (request.departmentId() != null) {
            department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.departmentId()));
        }

        Employee employee = Employee.builder()
            .user(user)
            .firstName(request.firstName())
            .lastName(request.lastName())
            .position(request.position())
            .salary(request.salary())
            .hireDate(request.hireDate())
            .department(department)
            .build();

        Employee savedEmployee = employeeRepository.save(employee);
        log.info("Employee created successfully with id: {}", savedEmployee.getId());
        return toResponse(savedEmployee);
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, UpdateEmployeeRequest request) {
        log.info("Updating employee id: {}", id);
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));

        if (request.position() != null && !request.position().equals(employee.getPosition())) {
            recordHistory(employee, "POSITION", employee.getPosition(), request.position());
            employee.setPosition(request.position());
        }

        if (request.departmentId() != null) {
            Long oldDeptId = employee.getDepartment() != null ? employee.getDepartment().getId() : null;
            if (!request.departmentId().equals(oldDeptId)) {
                String oldDeptName = employee.getDepartment() != null ? employee.getDepartment().getName() : "None";
                Department newDept = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.departmentId()));
                recordHistory(employee, "DEPARTMENT", oldDeptName, newDept.getName());
                employee.setDepartment(newDept);
            }
        }

        if (request.salary() != null && request.salary().compareTo(employee.getSalary()) != 0) {
            recordHistory(employee, "SALARY", employee.getSalary().toPlainString(), request.salary().toPlainString());
            employee.setSalary(request.salary());
        }

        if (request.firstName() != null) {
            employee.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            employee.setLastName(request.lastName());
        }

        Employee saved = employeeRepository.save(employee);
        log.info("Employee updated: {}", id);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EmploymentHistoryResponse> getEmploymentHistory(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
        assertCanViewEmployee(employee);
        return historyRepository.findByEmployeeIdOrderByChangedAtDesc(employeeId).stream()
            .map(h -> new EmploymentHistoryResponse(h.getId(), h.getChangeType(),
                h.getOldValue(), h.getNewValue(), h.getChangedAt()))
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getAllEmployees(Pageable pageable) {
        return scopedSearch(null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> searchEmployees(String search, Long departmentId,
                                                  String position, Pageable pageable) {
        return scopedSearch(search, departmentId, position, pageable);
    }

    private Page<EmployeeResponse> scopedSearch(String search, Long departmentId,
                                                 String position, Pageable pageable) {
        Specification<Employee> spec = Specification.allOf();

        // Role-based scope restriction
        User actorUser = currentUserService.getCurrentUserEntity();
        if (actorUser.getRole() == Role.EMPLOYEE) {
            spec = spec.and(EmployeeSpecification.hasUserId(actorUser.getId()));
        } else if (actorUser.getRole() == Role.DEPARTMENT_MANAGER) {
            var scopedDeptIds = departmentScopeService.getManagedDepartmentIds(actorUser.getId());
            if (!scopedDeptIds.isEmpty()) {
                spec = spec.and(EmployeeSpecification.hasDepartmentIdIn(scopedDeptIds));
            } else {
                spec = spec.and(EmployeeSpecification.hasUserId(actorUser.getId()));
            }
        }

        // Apply user-provided filters
        if (search != null && !search.isBlank()) {
            spec = spec.and(EmployeeSpecification.hasNameContaining(search));
        }
        if (departmentId != null) {
            spec = spec.and(EmployeeSpecification.hasDepartmentId(departmentId));
        }
        if (position != null && !position.isBlank()) {
            spec = spec.and(EmployeeSpecification.hasPosition(position));
        }

        return employeeRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        assertCanViewEmployee(employee);
        return toResponse(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeDetailResponse getEmployeeDetail(Long id) {
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        assertCanViewEmployee(employee);
        return toDetailResponse(employee);
    }

    @Transactional
    public EmployeeResponse uploadPhoto(Long id, MultipartFile file) {
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        assertCanManagePhoto(employee);

        if (hasManagedStoredPhoto(employee.getPhotoUrl())) {
            fileStorageService.delete(employee.getPhotoUrl());
        }

        StoredFileMetadata storedPhoto = fileStorageService.store(file, "employee-photos", file.getOriginalFilename());
        employee.setPhotoUrl(storedPhoto.storageKey());
        Employee saved = employeeRepository.save(employee);

        User actorUser = currentUserService.getCurrentUserEntity();
        auditLogService.log(actorUser, "EMPLOYEE_PHOTO_UPLOADED", "Employee", saved.getId().toString(),
            "Photo uploaded for employee " + saved.getFirstName() + " " + saved.getLastName(), null);

        if (!saved.getUser().getId().equals(actorUser.getId())) {
            notificationService.create(saved.getUser(), NotificationType.SECURITY_EVENT,
                "Profile photo updated", "Your employee profile photo was updated.", "/profile");
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public StoredFileContent downloadPhoto(Long id) {
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        assertCanViewEmployee(employee);
        if (employee.getPhotoUrl() == null || employee.getPhotoUrl().isBlank()) {
            throw new ResourceNotFoundException("EmployeePhoto", "employeeId", id);
        }
        if (isFrontendAssetPhoto(employee.getPhotoUrl())) {
            return loadFrontendAssetPhoto(employee.getPhotoUrl(), employee.getFirstName() + "-" + employee.getLastName());
        }
        return fileStorageService.load(employee.getPhotoUrl(), employee.getFirstName() + "-" + employee.getLastName() + ".jpg");
    }

    @Transactional
    public EmployeeDocumentResponse uploadDocument(Long employeeId, String title, String description, MultipartFile file) {
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
        assertCanManageDocuments(employee);
        validateFile(file);

        User actorUser = currentUserService.getCurrentUserEntity();
        StoredFileMetadata storedFile = fileStorageService.store(file, "employee-documents/" + employeeId, file.getOriginalFilename());

        EmployeeDocument document = employeeDocumentRepository.save(EmployeeDocument.builder()
            .employee(employee)
            .uploadedByUser(actorUser)
            .documentType(EmployeeDocumentType.HR_DOCUMENT)
            .title(title)
            .description(description)
            .originalFilename(storedFile.originalFilename())
            .contentType(storedFile.contentType())
            .storageKey(storedFile.storageKey())
            .fileSize(storedFile.contentLength())
            .build());

        auditLogService.log(actorUser, "EMPLOYEE_DOCUMENT_UPLOADED", "EmployeeDocument", document.getId().toString(),
            "Document uploaded for employee " + employee.getFirstName() + " " + employee.getLastName(), title);

        notificationService.create(employee.getUser(), NotificationType.SECURITY_EVENT,
            "New employee document", "A new document was added to your profile: " + title, "/profile");

        return toDocumentResponse(document);
    }

    @Transactional(readOnly = true)
    public List<EmployeeDocumentResponse> getEmployeeDocuments(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
        assertCanViewDocuments(employee);
        return employeeDocumentRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId).stream()
            .map(this::toDocumentResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public StoredFileContent downloadDocument(Long employeeId, Long documentId) {
        EmployeeDocument document = employeeDocumentRepository.findByIdAndEmployeeId(documentId, employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("EmployeeDocument", "id", documentId));
        assertCanViewDocuments(document.getEmployee());
        return fileStorageService.load(document.getStorageKey(), document.getOriginalFilename());
    }

    @Transactional(readOnly = true)
    public String exportToCsv() {
        List<Employee> employees = employeeRepository.findAllWithUserAndDepartment();
        StringBuilder sb = new StringBuilder();
        sb.append("ID,First Name,Last Name,Email,Position,Department,Salary,Hire Date\n");

        for (Employee emp : employees) {
            sb.append(emp.getId()).append(",");
            sb.append(escapeCsv(emp.getFirstName())).append(",");
            sb.append(escapeCsv(emp.getLastName())).append(",");
            sb.append(escapeCsv(emp.getUser().getEmail())).append(",");
            sb.append(escapeCsv(emp.getPosition())).append(",");
            sb.append(escapeCsv(emp.getDepartment() != null ? emp.getDepartment().getName() : "")).append(",");
            sb.append(emp.getSalary()).append(",");
            sb.append(emp.getHireDate()).append("\n");
        }

        return sb.toString();
    }

    @Transactional
    public void deleteEmployee(Long id) {
        log.info("Deleting employee with id: {}", id);
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));

        // 1. Delete file storage artifacts
        if (hasManagedStoredPhoto(employee.getPhotoUrl())) {
            fileStorageService.delete(employee.getPhotoUrl());
        }
        employeeDocumentRepository.findByEmployeeIdOrderByCreatedAtDesc(id)
            .forEach(document -> fileStorageService.delete(document.getStorageKey()));

        // 2. Delete dependent records in correct FK order
        leaveRequestHistoryRepository.deleteByLeaveRequestEmployeeId(id);
        leaveRequestRepository.deleteByEmployeeId(id);
        leaveBalanceRepository.deleteByEmployeeId(id);
        payrollComponentRepository.deleteByPayrollRecordEmployeeId(id);
        payrollRepository.deleteByEmployeeId(id);
        historyRepository.deleteByEmployeeId(id);
        employeeDocumentRepository.deleteByEmployeeId(id);

        // 3. Delete the employee itself
        employeeRepository.delete(employee);
        log.info("Employee deleted successfully with id: {}", id);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessRuleException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessRuleException("File size exceeds maximum allowed size of 10MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessRuleException("File type not allowed. Accepted: PDF, images (JPEG/PNG/GIF/WebP), Word, Excel");
        }
    }

    private void assertCanViewEmployee(Employee employee) {
        User actorUser = currentUserService.getCurrentUserEntity();
        if (actorUser.getRole() == Role.ADMIN || actorUser.getRole() == Role.HR_MANAGER) {
            return;
        }

        Employee actorEmployee = employeeRepository.findByUserId(actorUser.getId()).orElse(null);
        if (actorEmployee != null && actorEmployee.getId().equals(employee.getId())) {
            return;
        }

        if (actorUser.getRole() == Role.DEPARTMENT_MANAGER && isInManagementScope(employee, actorUser)) {
            return;
        }

        throw new AccessDeniedException("You do not have access to this employee");
    }

    private void assertCanViewDocuments(Employee employee) {
        User actorUser = currentUserService.getCurrentUserEntity();
        if (actorUser.getRole() == Role.ADMIN || actorUser.getRole() == Role.HR_MANAGER) {
            return;
        }

        Employee actorEmployee = employeeRepository.findByUserId(actorUser.getId()).orElse(null);
        if (actorEmployee != null && actorEmployee.getId().equals(employee.getId())) {
            return;
        }

        throw new AccessDeniedException("You do not have access to these documents");
    }

    private void assertCanManageDocuments(Employee employee) {
        User actorUser = currentUserService.getCurrentUserEntity();
        if (actorUser.getRole() == Role.ADMIN || actorUser.getRole() == Role.HR_MANAGER) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to upload employee documents");
    }

    private void assertCanManagePhoto(Employee employee) {
        User actorUser = currentUserService.getCurrentUserEntity();
        if (actorUser.getRole() == Role.ADMIN || actorUser.getRole() == Role.HR_MANAGER) {
            return;
        }

        Employee actorEmployee = employeeRepository.findByUserId(actorUser.getId()).orElse(null);
        if (actorEmployee != null && actorEmployee.getId().equals(employee.getId())) {
            return;
        }

        throw new AccessDeniedException("You do not have permission to update this photo");
    }

    private boolean isInManagementScope(Employee employee, User actorUser) {
        return departmentScopeService.isEmployeeInManagedScope(employee, actorUser.getId());
    }

    private void recordHistory(Employee employee, String changeType, String oldValue, String newValue) {
        EmploymentHistory history = EmploymentHistory.builder()
            .employee(employee)
            .changeType(changeType)
            .oldValue(oldValue)
            .newValue(newValue)
            .build();
        historyRepository.save(history);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private boolean hasManagedStoredPhoto(String photoUrl) {
        return photoUrl != null && !photoUrl.isBlank() && !isFrontendAssetPhoto(photoUrl);
    }

    private boolean isFrontendAssetPhoto(String photoUrl) {
        return photoUrl != null && photoUrl.startsWith("/assets/");
    }

    private StoredFileContent loadFrontendAssetPhoto(String photoUrl, String fallbackFilename) {
        Path assetPath = resolveFrontendAssetPath(photoUrl);
        if (assetPath == null || !Files.isRegularFile(assetPath)) {
            throw new ResourceNotFoundException("EmployeePhoto", "photoUrl", photoUrl);
        }

        try {
            byte[] content = Files.readAllBytes(assetPath);
            String contentType = Files.probeContentType(assetPath);
            String filename = assetPath.getFileName() != null ? assetPath.getFileName().toString() : fallbackFilename + ".jpg";
            return new StoredFileContent(
                new InputStreamResource(new ByteArrayInputStream(content)),
                contentType != null ? contentType : "image/jpeg",
                content.length,
                filename
            );
        } catch (IOException exception) {
            throw new BusinessRuleException("Unable to load seeded employee photo from local assets");
        }
    }

    private Path resolveFrontendAssetPath(String photoUrl) {
        String normalizedPhotoUrl = photoUrl.startsWith("/") ? photoUrl.substring(1) : photoUrl;
        Path[] candidates = {
            Path.of("frontend").resolve(normalizedPhotoUrl),
            Path.of("..", "frontend").resolve(normalizedPhotoUrl),
            Path.of(normalizedPhotoUrl),
            Path.of("..").resolve(normalizedPhotoUrl)
        };

        for (Path candidate : candidates) {
            Path absolute = candidate.toAbsolutePath().normalize();
            if (Files.isRegularFile(absolute)) {
                return absolute;
            }
        }

        return null;
    }

    private EmployeeDetailResponse toDetailResponse(Employee employee) {
        List<EmploymentHistoryResponse> history = historyRepository.findByEmployeeIdOrderByChangedAtDesc(employee.getId()).stream()
            .map(h -> new EmploymentHistoryResponse(h.getId(), h.getChangeType(), h.getOldValue(), h.getNewValue(), h.getChangedAt()))
            .toList();

        List<EmployeeDocumentResponse> documents = employeeDocumentRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId()).stream()
            .map(this::toDocumentResponse)
            .toList();

        return new EmployeeDetailResponse(
            employee.getId(),
            employee.getUser().getEmail(),
            employee.getFirstName(),
            employee.getLastName(),
            employee.getPosition(),
            employee.getSalary(),
            employee.getHireDate(),
            employee.getPhotoUrl(),
            employee.getDepartment() != null ? employee.getDepartment().getId() : null,
            employee.getDepartment() != null ? employee.getDepartment().getName() : null,
            employee.getPhone(),
            employee.getAddress(),
            employee.getEmergencyContactName(),
            employee.getEmergencyContactPhone(),
            history,
            documents
        );
    }

    private EmployeeDocumentResponse toDocumentResponse(EmployeeDocument document) {
        return new EmployeeDocumentResponse(
            document.getId(),
            document.getDocumentType(),
            document.getTitle(),
            document.getDescription(),
            document.getOriginalFilename(),
            document.getContentType(),
            document.getFileSize(),
            document.getUploadedByUser() != null ? document.getUploadedByUser().getId() : null,
            document.getUploadedByUser() != null ? document.getUploadedByUser().getEmail() : null,
            document.getCreatedAt()
        );
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
            employee.getId(),
            employee.getUser().getEmail(),
            employee.getFirstName(),
            employee.getLastName(),
            employee.getPosition(),
            employee.getSalary(),
            employee.getHireDate(),
            employee.getPhotoUrl(),
            employee.getDepartment() != null ? employee.getDepartment().getId() : null,
            employee.getDepartment() != null ? employee.getDepartment().getName() : null,
            employee.getPhone(),
            employee.getAddress(),
            employee.getEmergencyContactName(),
            employee.getEmergencyContactPhone()
        );
    }
}
