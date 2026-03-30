package com.hrpilot.backend.payroll;

import com.hrpilot.backend.audit.AuditLogService;
import com.hrpilot.backend.common.exception.BusinessRuleException;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.common.storage.FileStorageService;
import com.hrpilot.backend.common.storage.StoredFileContent;
import com.hrpilot.backend.common.storage.StoredFileMetadata;
import com.hrpilot.backend.employee.Employee;
import com.hrpilot.backend.employee.EmployeeRepository;
import com.hrpilot.backend.notification.NotificationService;
import com.hrpilot.backend.notification.NotificationType;
import com.hrpilot.backend.payroll.dto.CreatePayrollRequest;
import com.hrpilot.backend.payroll.dto.CreatePayrollRunRequest;
import com.hrpilot.backend.payroll.dto.PayrollComponentResponse;
import com.hrpilot.backend.payroll.dto.PayrollPreviewRequest;
import com.hrpilot.backend.payroll.dto.PayrollResponse;
import com.hrpilot.backend.payroll.dto.PayrollRunResponse;
import com.hrpilot.backend.user.CurrentUserService;
import com.hrpilot.backend.user.Role;
import com.hrpilot.backend.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollService {
    private final PayrollRepository payrollRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final PayrollComponentRepository payrollComponentRepository;
    private final EmployeeRepository employeeRepository;
    private final PayrollCalculatorService payrollCalculatorService;
    private final PayslipService payslipService;
    private final FileStorageService fileStorageService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional
    public PayrollResponse createPayroll(CreatePayrollRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.employeeId()));

        validatePeriod(request.year(), request.month());
        if (payrollRepository.existsByEmployeeIdAndYearAndMonth(employee.getId(), request.year(), request.month())) {
            throw new BusinessRuleException("Payroll already exists for this employee and period");
        }

        PayrollCalculationResult calculation = payrollCalculatorService.calculate(
            employee,
            request.bonus(),
            request.deductions(),
            request.taxClass()
        );

        PayrollRecord payrollRecord = createRecord(employee, request.year(), request.month(), null, calculation, request.bonus(), request.deductions());
        PayrollRecord saved = payrollRepository.save(payrollRecord);
        persistComponents(saved, calculation.components());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PayrollResponse previewPayroll(PayrollPreviewRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.employeeId()));
        validatePeriod(request.year(), request.month());

        PayrollCalculationResult calculation = payrollCalculatorService.calculate(
            employee,
            request.bonus(),
            request.additionalDeduction(),
            request.taxClass()
        );

        PayrollRecord previewRecord = createRecord(employee, request.year(), request.month(), null, calculation, request.bonus(), request.additionalDeduction());
        return toResponse(previewRecord, calculation.components().stream().map(this::toComponentResponse).toList());
    }

    @Transactional
    public PayrollRunResponse createPayrollRun(CreatePayrollRunRequest request) {
        validatePeriod(request.year(), request.month());
        if (payrollRunRepository.existsByNameAndYearAndMonth(request.name(), request.year(), request.month())) {
            throw new BusinessRuleException("A payroll run with this name already exists for the selected period");
        }

        User actorUser = currentUserService.getCurrentUserEntity();
        List<Employee> employees = resolveRunEmployees(request);
        if (employees.isEmpty()) {
            throw new BusinessRuleException("Payroll run must target at least one employee");
        }

        PayrollRun payrollRun = payrollRunRepository.save(PayrollRun.builder()
            .name(request.name())
            .year(request.year())
            .month(request.month())
            .status(PayrollRunStatus.DRAFT)
            .generatedByUser(actorUser)
            .build());

        BigDecimal bonus = request.bonus() != null ? request.bonus() : BigDecimal.ZERO;
        BigDecimal additionalDeduction = request.additionalDeduction() != null ? request.additionalDeduction() : BigDecimal.ZERO;

        for (Employee employee : employees) {
            if (payrollRepository.existsByEmployeeIdAndYearAndMonth(employee.getId(), request.year(), request.month())) {
                throw new BusinessRuleException("Payroll already exists for employee " + employee.getFirstName() + " " + employee.getLastName()
                    + " in " + request.month() + "/" + request.year());
            }
            PayrollCalculationResult calculation = payrollCalculatorService.calculate(employee, bonus, additionalDeduction, request.taxClass());
            PayrollRecord record = payrollRepository.save(createRecord(employee, request.year(), request.month(), payrollRun, calculation, bonus, additionalDeduction));
            persistComponents(record, calculation.components());
        }

        auditLogService.log(actorUser, "PAYROLL_RUN_CREATED", "PayrollRun", payrollRun.getId().toString(),
            "Payroll run created: " + payrollRun.getName(), null);

        return toRunResponse(payrollRun);
    }

    public Page<PayrollResponse> getAllPayrolls(Pageable pageable) {
        return payrollRepository.findAll(pageable).map(this::toSummaryResponse);
    }

    public Page<PayrollRunResponse> getPayrollRuns(Pageable pageable) {
        return payrollRunRepository.findAll(pageable).map(this::toRunResponse);
    }

    public List<PayrollResponse> getPayrollsByEmployee(Long employeeId) {
        assertCanViewEmployeePayroll(employeeId);
        return payrollRepository.findByEmployeeIdOrderByYearDescMonthDesc(employeeId).stream()
            .map(this::toSummaryResponse)
            .toList();
    }

    public List<PayrollResponse> getCurrentUserPayrolls() {
        User actorUser = currentUserService.getCurrentUserEntity();
        return employeeRepository.findByUserId(actorUser.getId())
            .map(employee -> payrollRepository.findByEmployeeIdOrderByYearDescMonthDesc(employee.getId()).stream()
                .map(this::toSummaryResponse)
                .toList())
            .orElse(List.of());
    }

    public List<PayrollComponentResponse> getComponents(Long payrollId) {
        PayrollRecord record = getVisiblePayroll(payrollId);
        return payrollComponentRepository.findByPayrollRecordIdOrderByIdAsc(record.getId()).stream()
            .map(this::toComponentResponse)
            .toList();
    }

    @Transactional
    public PayrollResponse markAsPaid(Long id) {
        PayrollRecord payrollRecord = payrollRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("PayrollRecord", "id", id));
        if (payrollRecord.getStatus() == PayrollStatus.PAID) {
            throw new BusinessRuleException("Payroll record is already marked as paid");
        }
        if (payrollRecord.getStatus() == PayrollStatus.DRAFT) {
            publishSingleRecord(payrollRecord);
        }

        payrollRecord.setStatus(PayrollStatus.PAID);
        payrollRecord.setPaidAt(LocalDateTime.now());
        PayrollRecord saved = payrollRepository.save(payrollRecord);
        notifyEmployee(saved, "Payroll marked as paid", "Your payroll for " + saved.getMonth() + "/" + saved.getYear() + " has been marked as paid.");
        return toResponse(saved);
    }

    @Transactional
    public PayrollRunResponse publishRun(Long runId) {
        PayrollRun run = payrollRunRepository.findById(runId)
            .orElseThrow(() -> new ResourceNotFoundException("PayrollRun", "id", runId));
        if (run.getStatus() != PayrollRunStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT payroll runs can be published");
        }

        for (PayrollRecord record : payrollRepository.findByRunIdOrderByEmployeeIdAsc(runId)) {
            publishSingleRecord(record);
        }

        run.setStatus(PayrollRunStatus.PUBLISHED);
        run.setPublishedAt(LocalDateTime.now());
        return toRunResponse(payrollRunRepository.save(run));
    }

    @Transactional
    public PayrollRunResponse payRun(Long runId) {
        PayrollRun run = payrollRunRepository.findById(runId)
            .orElseThrow(() -> new ResourceNotFoundException("PayrollRun", "id", runId));
        if (run.getStatus() == PayrollRunStatus.PAID) {
            throw new BusinessRuleException("Payroll run is already paid");
        }

        if (run.getStatus() == PayrollRunStatus.DRAFT) {
            publishRun(runId);
            run = payrollRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRun", "id", runId));
        }

        for (PayrollRecord record : payrollRepository.findByRunIdOrderByEmployeeIdAsc(runId)) {
            if (record.getStatus() != PayrollStatus.PAID) {
                record.setStatus(PayrollStatus.PAID);
                record.setPaidAt(LocalDateTime.now());
                payrollRepository.save(record);
                notifyEmployee(record, "Payroll paid", "Your payroll for " + record.getMonth() + "/" + record.getYear() + " has been paid.");
            }
        }

        run.setStatus(PayrollRunStatus.PAID);
        run.setPaidAt(LocalDateTime.now());
        return toRunResponse(payrollRunRepository.save(run));
    }

    public StoredFileContent downloadPayslip(Long payrollId) {
        PayrollRecord record = getVisiblePayroll(payrollId);
        return loadPayslip(record);
    }

    public StoredFileContent downloadPayslipForCurrentUser(Long payrollId) {
        PayrollRecord record = payrollRepository.findById(payrollId)
            .orElseThrow(() -> new ResourceNotFoundException("PayrollRecord", "id", payrollId));

        User actorUser = currentUserService.getCurrentUserEntity();
        Employee actorEmployee = employeeRepository.findByUserId(actorUser.getId())
            .orElseThrow(() -> new AccessDeniedException("Current account is not linked to an employee record"));

        if (!actorEmployee.getId().equals(record.getEmployee().getId())) {
            throw new AccessDeniedException("You do not have access to this payslip");
        }

        return loadPayslip(record);
    }

    private void publishSingleRecord(PayrollRecord record) {
        if (record.getStatus() == PayrollStatus.PUBLISHED || record.getStatus() == PayrollStatus.PAID) {
            return;
        }

        List<PayrollComponent> components = payrollComponentRepository.findByPayrollRecordIdOrderByIdAsc(record.getId());
        byte[] pdfBytes = payslipService.createPayslipPdf(record, components);
        String filename = "payslip-" + record.getEmployee().getId() + "-" + record.getYear() + "-" + record.getMonth() + ".pdf";
        StoredFileMetadata storedFile = fileStorageService.store(pdfBytes, "payslips/" + record.getYear() + "/" + record.getMonth(), filename, "application/pdf");

        record.setPayslipStorageKey(storedFile.storageKey());
        record.setPayslipFilename(storedFile.originalFilename());
        record.setStatus(PayrollStatus.PUBLISHED);
        record.setPublishedAt(LocalDateTime.now());
        payrollRepository.save(record);

        notifyEmployee(record, "Payroll published", "A new payslip is available for " + record.getMonth() + "/" + record.getYear() + ".");
    }

    private StoredFileContent loadPayslip(PayrollRecord record) {
        if (record.getPayslipStorageKey() == null || record.getPayslipFilename() == null) {
            throw new ResourceNotFoundException("Payslip", "payrollId", record.getId());
        }
        return fileStorageService.load(record.getPayslipStorageKey(), record.getPayslipFilename());
    }

    private PayrollRecord getVisiblePayroll(Long payrollId) {
        PayrollRecord record = payrollRepository.findById(payrollId)
            .orElseThrow(() -> new ResourceNotFoundException("PayrollRecord", "id", payrollId));

        User actorUser = currentUserService.getCurrentUserEntity();
        if (actorUser.getRole() == Role.ADMIN || actorUser.getRole() == Role.HR_MANAGER) {
            return record;
        }

        Employee actorEmployee = employeeRepository.findByUserId(actorUser.getId())
            .orElseThrow(() -> new AccessDeniedException("Current account is not linked to an employee record"));
        if (!actorEmployee.getId().equals(record.getEmployee().getId())) {
            throw new AccessDeniedException("You do not have access to this payroll");
        }

        return record;
    }

    private void assertCanViewEmployeePayroll(Long employeeId) {
        User actorUser = currentUserService.getCurrentUserEntity();
        if (actorUser.getRole() == Role.ADMIN || actorUser.getRole() == Role.HR_MANAGER) {
            return;
        }

        Employee actorEmployee = employeeRepository.findByUserId(actorUser.getId())
            .orElseThrow(() -> new AccessDeniedException("Current account is not linked to an employee record"));
        if (!actorEmployee.getId().equals(employeeId)) {
            throw new AccessDeniedException("You do not have access to this payroll list");
        }
    }

    private List<Employee> resolveRunEmployees(CreatePayrollRunRequest request) {
        if (Boolean.TRUE.equals(request.includeAllEmployees())) {
            return employeeRepository.findAll();
        }
        if (request.employeeIds() == null || request.employeeIds().isEmpty()) {
            return List.of();
        }
        List<Employee> employees = new ArrayList<>();
        for (Long employeeId : request.employeeIds()) {
            employees.add(employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId)));
        }
        return employees;
    }

    private void validatePeriod(int year, int month) {
        if (month < 1 || month > 12) {
            throw new BusinessRuleException("Month must be between 1 and 12");
        }
        if (year < 2000 || year > 2100) {
            throw new BusinessRuleException("Year must be between 2000 and 2100");
        }
    }

    private PayrollRecord createRecord(Employee employee, int year, int month, PayrollRun run,
                                       PayrollCalculationResult calculation, BigDecimal bonus, BigDecimal additionalDeduction) {
        BigDecimal normalizedBonus = bonus != null ? bonus : BigDecimal.ZERO;
        BigDecimal normalizedAdditionalDeduction = additionalDeduction != null ? additionalDeduction : BigDecimal.ZERO;
        return PayrollRecord.builder()
            .employee(employee)
            .run(run)
            .year(year)
            .month(month)
            .baseSalary(calculation.monthlyBaseSalary())
            .grossSalary(calculation.grossSalary())
            .bonus(normalizedBonus)
            .deductions(calculation.totalDeductions())
            .employeeSocialContributions(calculation.employeeSocialContributions())
            .employerSocialContributions(calculation.employerSocialContributions())
            .incomeTax(calculation.incomeTax())
            .taxClass(calculation.taxClass())
            .netSalary(calculation.netSalary())
            .status(PayrollStatus.DRAFT)
            .build();
    }

    private void persistComponents(PayrollRecord payrollRecord, List<PayrollCalculationResult.PayrollComponentDraft> components) {
        for (PayrollCalculationResult.PayrollComponentDraft draft : components) {
            payrollComponentRepository.save(PayrollComponent.builder()
                .payrollRecord(payrollRecord)
                .componentType(draft.componentType())
                .code(draft.code())
                .label(draft.label())
                .amount(draft.amount())
                .build());
        }
    }

    private PayrollComponentResponse toComponentResponse(PayrollComponent component) {
        return new PayrollComponentResponse(
            component.getId(),
            component.getComponentType(),
            component.getCode(),
            component.getLabel(),
            component.getAmount()
        );
    }

    private PayrollComponentResponse toComponentResponse(PayrollCalculationResult.PayrollComponentDraft component) {
        return new PayrollComponentResponse(
            null,
            component.componentType(),
            component.code(),
            component.label(),
            component.amount()
        );
    }

    private PayrollRunResponse toRunResponse(PayrollRun run) {
        return new PayrollRunResponse(
            run.getId(),
            run.getName(),
            run.getYear(),
            run.getMonth(),
            run.getStatus(),
            (int) payrollRepository.countByRunId(run.getId()),
            run.getCreatedAt(),
            run.getPublishedAt(),
            run.getPaidAt()
        );
    }

    private PayrollResponse toResponse(PayrollRecord record) {
        return toResponse(record, payrollComponentRepository.findByPayrollRecordIdOrderByIdAsc(record.getId()).stream()
            .map(this::toComponentResponse)
            .toList());
    }

    private PayrollResponse toSummaryResponse(PayrollRecord record) {
        return toResponse(record, List.of());
    }

    private PayrollResponse toResponse(PayrollRecord record, List<PayrollComponentResponse> components) {
        return new PayrollResponse(
            record.getId(),
            record.getEmployee().getId(),
            record.getEmployee().getFirstName() + " " + record.getEmployee().getLastName(),
            record.getYear(),
            record.getMonth(),
            record.getBaseSalary(),
            record.getGrossSalary(),
            record.getBonus(),
            record.getDeductions(),
            record.getEmployeeSocialContributions(),
            record.getEmployerSocialContributions(),
            record.getIncomeTax(),
            record.getNetSalary(),
            record.getTaxClass(),
            record.getStatus(),
            record.getRun() != null ? record.getRun().getId() : null,
            record.getPublishedAt(),
            record.getPaidAt(),
            record.getPayslipStorageKey() != null,
            components
        );
    }

    private void notifyEmployee(PayrollRecord record, String title, String message) {
        notificationService.create(record.getEmployee().getUser(), NotificationType.PAYROLL_EVENT, title, message, "/payrolls");
    }
}
