package com.hrpilot.backend.payroll;

import com.hrpilot.backend.employee.Employee;
import com.hrpilot.backend.employee.EmployeeRepository;
import com.hrpilot.backend.payroll.dto.CreatePayrollRequest;
import com.hrpilot.backend.payroll.dto.PayrollResponse;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.common.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollService {
    private final PayrollRepository payrollRepository;
    private final EmployeeRepository employeeRepository;

    public PayrollResponse createPayroll(CreatePayrollRequest request) {
        log.info("Creating payroll for employee id: {}, month: {}/{}", request.employeeId(), request.month(), request.year());
        Employee employee =
    employeeRepository.findById(request.employeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.employeeId()));

        if (request.month() < 1 || request.month() > 12) {
            throw new BusinessRuleException("Month must be between 1 and 12");
        }

        BigDecimal netSalary = request.baseSalary()
            .add(request.bonus())
            .subtract(request.deductions());
        PayrollRecord payrollRecord = PayrollRecord.builder()
                .employee(employee)
                .year(request.year())
                .month(request.month())
                .baseSalary(request.baseSalary())
                .bonus(request.bonus())
                .deductions(request.deductions())
                .netSalary(netSalary)
                .build();

        PayrollRecord saved = payrollRepository.save(payrollRecord);
        log.info("Payroll created successfully with id: {}", saved.getId());
        return toResponse(saved);
    }

    public Page<PayrollResponse> getAllPayrolls(Pageable pageable) {
        return payrollRepository.findAll(pageable)
                .map(this::toResponse);
    }

    public List<PayrollResponse> getPayrollsByEmployee(Long employeeId) {
        return payrollRepository.findByEmployeeId(employeeId).stream()
                .map(this::toResponse)
                .toList();
    }

    public PayrollResponse markAsPaid(Long id) {
        log.info("Marking payroll as paid with id: {}", id);
        PayrollRecord payrollRecord = payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRecord", "id", id));
        if (payrollRecord.getStatus() == PayrollStatus.PAID) {
            throw new BusinessRuleException("Payroll record is already marked as paid");
        }
        payrollRecord.setStatus(PayrollStatus.PAID);
        return toResponse(payrollRepository.save(payrollRecord));
    }

    private PayrollResponse toResponse(PayrollRecord pr) {
        return new PayrollResponse(
                pr.getId(),
                pr.getEmployee().getId(),
                pr.getEmployee().getFirstName() + " " + pr.getEmployee().getLastName(),
                pr.getYear(),
                pr.getMonth(),
                pr.getBaseSalary(),
                pr.getBonus(),
                pr.getDeductions(),
                pr.getNetSalary(),
                pr.getStatus()
        );
    }
}