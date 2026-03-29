package com.hrpilot.backend.leave;

import com.hrpilot.backend.common.exception.BusinessRuleException;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.employee.Employee;
import com.hrpilot.backend.employee.EmployeeRepository;
import com.hrpilot.backend.leave.dto.LeaveBalanceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;

    private static final int DEFAULT_ANNUAL_DAYS = 30;
    private static final int DEFAULT_SICK_DAYS = 15;
    private static final int DEFAULT_UNPAID_DAYS = 10;

    public List<LeaveBalanceResponse> getBalances(Long employeeId, int year) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee", "id", employeeId);
        }

        List<LeaveBalance> balances = leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year);

        // Auto-initialize balances if not set for this year
        if (balances.isEmpty()) {
            balances = initializeBalances(employeeId, year);
        }

        return balances.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void deductBalance(Long employeeId, LeaveType leaveType, LocalDate startDate, LocalDate endDate) {
        int year = startDate.getYear();
        int days = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;

        LeaveBalance balance = leaveBalanceRepository
            .findByEmployeeIdAndLeaveTypeAndYear(employeeId, leaveType, year)
            .orElseGet(() -> {
                List<LeaveBalance> initialized = initializeBalances(employeeId, year);
                return initialized.stream()
                    .filter(b -> b.getLeaveType() == leaveType)
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException("Could not initialize balance"));
            });

        if (balance.getRemainingDays() < days) {
            throw new BusinessRuleException(
                "Insufficient " + leaveType + " balance. Remaining: " + balance.getRemainingDays()
                + ", Requested: " + days);
        }

        balance.setUsedDays(balance.getUsedDays() + days);
        leaveBalanceRepository.save(balance);
        log.info("Deducted {} days from {} balance for employee {}", days, leaveType, employeeId);
    }

    @Transactional
    public void restoreBalance(Long employeeId, LeaveType leaveType, LocalDate startDate, LocalDate endDate) {
        int year = startDate.getYear();
        int days = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;

        leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(employeeId, leaveType, year)
            .ifPresent(balance -> {
                balance.setUsedDays(Math.max(0, balance.getUsedDays() - days));
                leaveBalanceRepository.save(balance);
                log.info("Restored {} days to {} balance for employee {}", days, leaveType, employeeId);
            });
    }

    private List<LeaveBalance> initializeBalances(Long employeeId, int year) {
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        List<LeaveBalance> balances = List.of(
            createBalance(employee, LeaveType.ANNUAL, year, DEFAULT_ANNUAL_DAYS),
            createBalance(employee, LeaveType.SICK, year, DEFAULT_SICK_DAYS),
            createBalance(employee, LeaveType.UNPAID, year, DEFAULT_UNPAID_DAYS)
        );

        return leaveBalanceRepository.saveAll(balances);
    }

    private LeaveBalance createBalance(Employee employee, LeaveType type, int year, int totalDays) {
        return LeaveBalance.builder()
            .employee(employee)
            .leaveType(type)
            .year(year)
            .totalDays(totalDays)
            .usedDays(0)
            .build();
    }

    private LeaveBalanceResponse toResponse(LeaveBalance balance) {
        return new LeaveBalanceResponse(
            balance.getId(),
            balance.getEmployee().getId(),
            balance.getLeaveType(),
            balance.getYear(),
            balance.getTotalDays(),
            balance.getUsedDays(),
            balance.getRemainingDays()
        );
    }
}
