package com.hrpilot.backend.leave;

import com.hrpilot.backend.common.exception.BusinessRuleException;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.department.DepartmentScopeService;
import com.hrpilot.backend.employee.Employee;
import com.hrpilot.backend.employee.EmployeeRepository;
import com.hrpilot.backend.leave.dto.LeaveBalanceResponse;
import com.hrpilot.backend.user.CurrentUserService;
import com.hrpilot.backend.user.Role;
import com.hrpilot.backend.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeavePolicyService leavePolicyService;
    private final CurrentUserService currentUserService;
    private final DepartmentScopeService departmentScopeService;

    public List<LeaveBalanceResponse> getBalances(Long employeeId, int year) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee", "id", employeeId);
        }

        List<LeaveBalance> balances = leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year);

        if (balances.isEmpty()) {
            balances = initializeBalances(employeeId, year);
        }

        return balances.stream().map(this::toResponse).toList();
    }

    public List<LeaveBalanceResponse> getBalancesForEmployee(Long employeeId, int year) {
        User actorUser = currentUserService.getCurrentUserEntity();
        assertCanViewEmployee(employeeId, actorUser);
        return getBalances(employeeId, year);
    }

    public List<LeaveBalanceResponse> getCurrentUserBalances(int year) {
        User actorUser = currentUserService.getCurrentUserEntity();
        return employeeRepository.findByUserId(actorUser.getId())
            .map(employee -> getBalances(employee.getId(), year))
            .orElse(List.of());
    }

    @Transactional
    public void deductBalance(Long employeeId, LeaveType leaveType, int year, int days) {
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
    public void restoreBalance(Long employeeId, LeaveType leaveType, int year, int days) {
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

        Map<LeaveType, LeavePolicy> policies = leavePolicyService.getPoliciesByType();
        List<LeaveBalance> balances = List.of(
            createBalance(employee, LeaveType.ANNUAL, year, policies.getOrDefault(LeaveType.ANNUAL, null)),
            createBalance(employee, LeaveType.SICK, year, policies.getOrDefault(LeaveType.SICK, null)),
            createBalance(employee, LeaveType.UNPAID, year, policies.getOrDefault(LeaveType.UNPAID, null))
        );

        return leaveBalanceRepository.saveAll(balances);
    }

    private LeaveBalance createBalance(Employee employee, LeaveType type, int year, LeavePolicy policy) {
        return LeaveBalance.builder()
            .employee(employee)
            .leaveType(type)
            .year(year)
            .totalDays(policy != null ? policy.getAnnualDays() : leavePolicyService.getAnnualDays(type))
            .usedDays(0)
            .build();
    }

    private void assertCanViewEmployee(Long employeeId, User actorUser) {
        if (actorUser.getRole() == Role.ADMIN || actorUser.getRole() == Role.HR_MANAGER) {
            return;
        }

        Employee targetEmployee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        Employee actorEmployee = employeeRepository.findByUserId(actorUser.getId()).orElse(null);
        if (actorEmployee != null && actorEmployee.getId().equals(employeeId)) {
            return;
        }

        if (actorUser.getRole() == Role.DEPARTMENT_MANAGER && isInManagementScope(targetEmployee, actorUser)) {
            return;
        }

        throw new AccessDeniedException("You do not have access to these leave balances");
    }

    private boolean isInManagementScope(Employee employee, User actorUser) {
        return departmentScopeService.isEmployeeInManagedScope(employee, actorUser.getId());
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
