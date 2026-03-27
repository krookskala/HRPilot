package com.hrpilot.backend.payroll;

import com.hrpilot.backend.employee.Employee;
import com.hrpilot.backend.employee.EmployeeRepository;
import com.hrpilot.backend.payroll.dto.CreatePayrollRequest;
import com.hrpilot.backend.payroll.dto.PayrollResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PayrollService {
    private final PayrollRepository payrollRepository;
    private final EmployeeRepository employeeRepository;

    public PayrollResponse createPayroll(CreatePayrollRequest request) {
        Employee employee =
    employeeRepository.findById(request.employeeId())
                    .orElseThrow(() -> new RuntimeException("Employee Not Found"));

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

        return toResponse(payrollRepository.save(payrollRecord));
    }

    public List<PayrollResponse> getAllPayrolls() {
        return payrollRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<PayrollResponse> getPayrollsByEmployee(Long employeeId) {
        return payrollRepository.findByEmployeeId(employeeId).stream()
                .map(this::toResponse)
                .toList();
    }

    public PayrollResponse markAsPaid(Long id) {
        PayrollRecord payrollRecord = payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll Record Not Found"));
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