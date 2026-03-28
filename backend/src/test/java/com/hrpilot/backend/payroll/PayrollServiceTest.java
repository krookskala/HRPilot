package com.hrpilot.backend.payroll;

import com.hrpilot.backend.employee.Employee;
import com.hrpilot.backend.employee.EmployeeRepository;
import com.hrpilot.backend.payroll.dto.CreatePayrollRequest;
import com.hrpilot.backend.payroll.dto.PayrollResponse;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.common.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock
    private PayrollRepository payrollRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private PayrollService payrollService;

    private Employee buildEmployee() {
        return Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    private PayrollRecord buildPayrollRecord(Employee employee, PayrollStatus status) {
        return PayrollRecord.builder()
                .id(1L)
                .employee(employee)
                .year(2026)
                .month(3)
                .baseSalary(new BigDecimal("5000"))
                .bonus(new BigDecimal("500"))
                .deductions(new BigDecimal("300"))
                .netSalary(new BigDecimal("5200"))
                .status(status)
                .build();
    }

    @Test
    void createPayroll_success() {
        // Arrange
        Employee employee = buildEmployee();
        CreatePayrollRequest request = new CreatePayrollRequest(
                1L, 2026, 3,
                new BigDecimal("5000"),
                new BigDecimal("500"),
                new BigDecimal("300")
        );
        PayrollRecord saved = buildPayrollRecord(employee, PayrollStatus.DRAFT);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(payrollRepository.save(any(PayrollRecord.class))).thenReturn(saved);

        // Act
        PayrollResponse response = payrollService.createPayroll(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(2026, response.year());
        assertEquals(3, response.month());
        assertEquals(new BigDecimal("5000"), response.baseSalary());
        assertEquals(new BigDecimal("5200"), response.netSalary());
        assertEquals(PayrollStatus.DRAFT, response.status());
        assertEquals("John Doe", response.employeeFullName());
    }

    @Test
    void createPayroll_employeeNotFound_throwsException() {
        // Arrange
        CreatePayrollRequest request = new CreatePayrollRequest(
                99L, 2026, 3,
                new BigDecimal("5000"),
                new BigDecimal("500"),
                new BigDecimal("300")
        );
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> payrollService.createPayroll(request));
    }

    @Test
    void getAllPayrolls_returnsAll() {
        // Arrange
        Employee employee = buildEmployee();
        PayrollRecord record = buildPayrollRecord(employee, PayrollStatus.DRAFT);
        Pageable pageable = PageRequest.of(0, 10);
        Page<PayrollRecord> page = new PageImpl<>(List.of(record), pageable, 1);
        when(payrollRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<PayrollResponse> responses = payrollService.getAllPayrolls(pageable);

        // Assert
        assertEquals(1, responses.getTotalElements());
        assertEquals(2026, responses.getContent().get(0).year());
        assertEquals(3, responses.getContent().get(0).month());
    }

    @Test
    void getPayrollsByEmployee_success() {
        // Arrange
        Employee employee = buildEmployee();
        PayrollRecord record = buildPayrollRecord(employee, PayrollStatus.DRAFT);
        when(payrollRepository.findByEmployeeId(1L)).thenReturn(List.of(record));

        // Act
        List<PayrollResponse> responses = payrollService.getPayrollsByEmployee(1L);

        // Assert
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).employeeId());
        assertEquals("John Doe", responses.get(0).employeeFullName());
    }

    @Test
    void markAsPaid_success() {
        // Arrange
        Employee employee = buildEmployee();
        PayrollRecord record = buildPayrollRecord(employee, PayrollStatus.DRAFT);
        when(payrollRepository.findById(1L)).thenReturn(Optional.of(record));
        when(payrollRepository.save(any(PayrollRecord.class))).thenReturn(record);

        // Act
        PayrollResponse response = payrollService.markAsPaid(1L);

        // Assert
        assertNotNull(response);
        assertEquals(PayrollStatus.PAID, response.status());
    }

    @Test
    void markAsPaid_notFound_throwsException() {
        // Arrange
        when(payrollRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> payrollService.markAsPaid(99L));
    }

    @Test
    void createPayroll_invalidMonth_throwsException() {
        // Arrange
        Employee employee = buildEmployee();
        CreatePayrollRequest request = new CreatePayrollRequest(
                1L, 2026, 13,
                new BigDecimal("5000"),
                new BigDecimal("500"),
                new BigDecimal("300")
        );
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> payrollService.createPayroll(request));
    }

    @Test
    void markAsPaid_alreadyPaid_throwsException() {
        // Arrange
        Employee employee = buildEmployee();
        PayrollRecord record = buildPayrollRecord(employee, PayrollStatus.PAID);
        when(payrollRepository.findById(1L)).thenReturn(Optional.of(record));

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> payrollService.markAsPaid(1L));
    }
}
