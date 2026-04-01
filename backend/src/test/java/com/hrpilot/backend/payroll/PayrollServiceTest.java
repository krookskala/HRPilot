package com.hrpilot.backend.payroll;

import com.hrpilot.backend.audit.AuditLogService;
import com.hrpilot.backend.common.exception.BusinessRuleException;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.common.storage.FileStorageService;
import com.hrpilot.backend.common.storage.StoredFileMetadata;
import com.hrpilot.backend.employee.Employee;
import com.hrpilot.backend.employee.EmployeeRepository;
import com.hrpilot.backend.notification.NotificationService;
import com.hrpilot.backend.payroll.dto.CreatePayrollRequest;
import com.hrpilot.backend.payroll.dto.PayrollResponse;
import com.hrpilot.backend.user.CurrentUserService;
import com.hrpilot.backend.user.Role;
import com.hrpilot.backend.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock
    private PayrollRepository payrollRepository;

    @Mock
    private PayrollRunRepository payrollRunRepository;

    @Mock
    private PayrollComponentRepository payrollComponentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PayrollCalculatorService payrollCalculatorService;

    @Mock
    private PayslipService payslipService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PayrollService payrollService;

    private User buildUser(Role role) {
        return User.builder()
            .id(10L)
            .email("john@example.com")
            .passwordHash("secret")
            .role(role)
            .active(true)
            .build();
    }

    private Employee buildEmployee() {
        return Employee.builder()
            .id(1L)
            .user(buildUser(Role.EMPLOYEE))
            .firstName("John")
            .lastName("Doe")
            .position("Engineer")
            .salary(new BigDecimal("60000.00"))
            .hireDate(LocalDate.of(2024, 1, 15))
            .build();
    }

    private PayrollCalculationResult buildCalculation() {
        return new PayrollCalculationResult(
            new BigDecimal("5000.00"),
            new BigDecimal("5500.00"),
            new BigDecimal("700.00"),
            new BigDecimal("710.00"),
            new BigDecimal("1100.00"),
            new BigDecimal("1800.00"),
            new BigDecimal("3700.00"),
            "I",
            List.of(
                new PayrollCalculationResult.PayrollComponentDraft(
                    PayrollComponentType.EARNING,
                    "BASE",
                    "Base Salary",
                    new BigDecimal("5000.00")
                ),
                new PayrollCalculationResult.PayrollComponentDraft(
                    PayrollComponentType.TAX,
                    "INCOME_TAX",
                    "Income Tax",
                    new BigDecimal("1100.00")
                )
            )
        );
    }

    private PayrollRecord buildPayrollRecord(Employee employee, PayrollStatus status) {
        return PayrollRecord.builder()
            .id(1L)
            .employee(employee)
            .year(2026)
            .month(3)
            .baseSalary(new BigDecimal("5000.00"))
            .grossSalary(new BigDecimal("5500.00"))
            .bonus(new BigDecimal("500.00"))
            .deductions(new BigDecimal("1800.00"))
            .employeeSocialContributions(new BigDecimal("700.00"))
            .employerSocialContributions(new BigDecimal("710.00"))
            .incomeTax(new BigDecimal("1100.00"))
            .netSalary(new BigDecimal("3700.00"))
            .taxClass("I")
            .status(status)
            .payslipStorageKey(status == PayrollStatus.DRAFT ? null : "payslips/2026/3/payslip.pdf")
            .payslipFilename(status == PayrollStatus.DRAFT ? null : "payslip.pdf")
            .build();
    }

    private CreatePayrollRequest buildRequest(int month) {
        return new CreatePayrollRequest(
            1L,
            2026,
            month,
            new BigDecimal("5000.00"),
            new BigDecimal("500.00"),
            new BigDecimal("300.00"),
            "I"
        );
    }

    @Test
    void createPayroll_success() {
        Employee employee = buildEmployee();
        PayrollRecord saved = buildPayrollRecord(employee, PayrollStatus.DRAFT);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(payrollRepository.existsByEmployeeIdAndYearAndMonth(1L, 2026, 3)).thenReturn(false);
        when(payrollCalculatorService.calculate(employee, new BigDecimal("500.00"), new BigDecimal("300.00"), "I"))
            .thenReturn(buildCalculation());
        when(payrollRepository.save(any(PayrollRecord.class))).thenReturn(saved);
        when(payrollComponentRepository.findByPayrollRecordIdOrderByIdAsc(1L)).thenReturn(List.of());

        PayrollResponse response = payrollService.createPayroll(buildRequest(3));

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(new BigDecimal("5500.00"), response.grossSalary());
        assertEquals(new BigDecimal("3700.00"), response.netSalary());
        assertEquals("I", response.taxClass());
        verify(payrollComponentRepository, times(2)).save(any(PayrollComponent.class));
    }

    @Test
    void createPayroll_employeeNotFound_throwsException() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> payrollService.createPayroll(buildRequest(3)));
    }

    @Test
    void createPayroll_invalidMonth_throwsException() {
        Employee employee = buildEmployee();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        assertThrows(BusinessRuleException.class, () -> payrollService.createPayroll(buildRequest(13)));
        verify(payrollCalculatorService, never()).calculate(any(), any(), any(), any());
    }

    @Test
    void getAllPayrolls_returnsAll() {
        Employee employee = buildEmployee();
        PayrollRecord record = buildPayrollRecord(employee, PayrollStatus.PUBLISHED);
        Pageable pageable = PageRequest.of(0, 10);
        Page<PayrollRecord> page = new PageImpl<>(List.of(record), pageable, 1);

        when(payrollRepository.findAll(pageable)).thenReturn(page);

        Page<PayrollResponse> responses = payrollService.getAllPayrolls(pageable);

        assertEquals(1, responses.getTotalElements());
        assertEquals(PayrollStatus.PUBLISHED, responses.getContent().getFirst().status());
        assertTrue(responses.getContent().getFirst().components().isEmpty());
    }

    @Test
    void getPayrollsByEmployee_success_forAdmin() {
        Employee employee = buildEmployee();
        PayrollRecord record = buildPayrollRecord(employee, PayrollStatus.PUBLISHED);

        when(currentUserService.getCurrentUserEntity()).thenReturn(buildUser(Role.ADMIN));
        Pageable pageable = PageRequest.of(0, 10);
        when(payrollRepository.findByEmployeeIdOrderByYearDescMonthDesc(1L, pageable))
            .thenReturn(new PageImpl<>(List.of(record)));

        Page<PayrollResponse> responses = payrollService.getPayrollsByEmployee(1L, pageable);

        assertEquals(1, responses.getContent().size());
        assertEquals(1L, responses.getContent().getFirst().employeeId());
        assertEquals("John Doe", responses.getContent().getFirst().employeeFullName());
    }

    @Test
    void markAsPaid_success_fromPublished() {
        Employee employee = buildEmployee();
        PayrollRecord record = buildPayrollRecord(employee, PayrollStatus.PUBLISHED);

        when(payrollRepository.findById(1L)).thenReturn(Optional.of(record));
        when(payrollRepository.save(any(PayrollRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(payrollComponentRepository.findByPayrollRecordIdOrderByIdAsc(1L)).thenReturn(List.of());

        PayrollResponse response = payrollService.markAsPaid(1L);

        assertNotNull(response);
        assertEquals(PayrollStatus.PAID, response.status());
        assertNotNull(response.paidAt());
        verify(notificationService).create(any(), any(), any(), any(), any());
    }

    @Test
    void markAsPaid_success_fromDraft_autopublishesPayslip() {
        Employee employee = buildEmployee();
        PayrollRecord record = buildPayrollRecord(employee, PayrollStatus.DRAFT);
        PayrollComponent component = PayrollComponent.builder()
            .id(1L)
            .payrollRecord(record)
            .componentType(PayrollComponentType.EARNING)
            .code("BASE")
            .label("Base Salary")
            .amount(new BigDecimal("5000.00"))
            .build();

        when(payrollRepository.findById(1L)).thenReturn(Optional.of(record));
        when(payrollComponentRepository.findByPayrollRecordIdOrderByIdAsc(1L)).thenReturn(List.of(component));
        when(payslipService.createPayslipPdf(any(), any())).thenReturn(new byte[] {1, 2, 3});
        when(fileStorageService.store(any(byte[].class), any(), any(), any()))
            .thenReturn(new StoredFileMetadata("payslips/2026/3/payslip.pdf", "payslip.pdf", "application/pdf", 3));
        when(payrollRepository.save(any(PayrollRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PayrollResponse response = payrollService.markAsPaid(1L);

        assertEquals(PayrollStatus.PAID, response.status());
        assertNotNull(response.publishedAt());
        assertNotNull(response.paidAt());
        assertTrue(response.hasPayslip());
    }

    @Test
    void markAsPaid_notFound_throwsException() {
        when(payrollRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> payrollService.markAsPaid(99L));
    }

    @Test
    void markAsPaid_alreadyPaid_throwsException() {
        Employee employee = buildEmployee();
        PayrollRecord record = buildPayrollRecord(employee, PayrollStatus.PAID);
        when(payrollRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThrows(BusinessRuleException.class, () -> payrollService.markAsPaid(1L));
    }

    @Test
    void createPayroll_duplicatePeriod_throwsException() {
        Employee employee = buildEmployee();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(payrollRepository.existsByEmployeeIdAndYearAndMonth(1L, 2026, 3)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> payrollService.createPayroll(buildRequest(3)));
        verify(payrollRepository, never()).save(any(PayrollRecord.class));
    }
}
