package com.hrpilot.backend.config;

import com.hrpilot.backend.department.Department;
import com.hrpilot.backend.department.DepartmentRepository;
import com.hrpilot.backend.employee.Employee;
import com.hrpilot.backend.employee.EmployeeRepository;
import com.hrpilot.backend.leave.LeaveRequest;
import com.hrpilot.backend.leave.LeaveRequestRepository;
import com.hrpilot.backend.leave.LeaveStatus;
import com.hrpilot.backend.leave.LeaveType;
import com.hrpilot.backend.payroll.PayrollRecord;
import com.hrpilot.backend.payroll.PayrollRepository;
import com.hrpilot.backend.payroll.PayrollStatus;
import com.hrpilot.backend.user.Role;
import com.hrpilot.backend.user.User;
import com.hrpilot.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PayrollRepository payrollRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already has data — skipping seed.");
            return;
        }

        log.info("Seeding database with demo data...");

        // ── Users ──────────────────────────────────────────
        User admin = createUser("admin@hrpilot.com", "admin123", Role.ADMIN);
        User hrManager = createUser("hr@hrpilot.com", "hr1234", Role.HR_MANAGER);
        User manager1 = createUser("john.doe@hrpilot.com", "pass1234", Role.DEPARTMENT_MANAGER);
        User manager2 = createUser("jane.smith@hrpilot.com", "pass1234", Role.DEPARTMENT_MANAGER);
        User emp1 = createUser("alice.johnson@hrpilot.com", "pass1234", Role.EMPLOYEE);
        User emp2 = createUser("bob.wilson@hrpilot.com", "pass1234", Role.EMPLOYEE);
        User emp3 = createUser("clara.mueller@hrpilot.com", "pass1234", Role.EMPLOYEE);
        User emp4 = createUser("david.schmidt@hrpilot.com", "pass1234", Role.EMPLOYEE);
        User emp5 = createUser("emma.fischer@hrpilot.com", "pass1234", Role.EMPLOYEE);
        User emp6 = createUser("frank.weber@hrpilot.com", "pass1234", Role.EMPLOYEE);

        // ── Departments ────────────────────────────────────
        Department engineering = createDepartment("Engineering", manager1, null);
        Department hr = createDepartment("Human Resources", hrManager, null);
        Department finance = createDepartment("Finance", manager2, null);
        Department frontend = createDepartment("Frontend", null, engineering);
        Department backend = createDepartment("Backend", null, engineering);

        // ── Employees ──────────────────────────────────────
        Employee empJohn = createEmployee(manager1, "John", "Doe", "Engineering Manager",
                new BigDecimal("85000"), LocalDate.of(2022, 3, 15), engineering);
        Employee empJane = createEmployee(manager2, "Jane", "Smith", "Finance Manager",
                new BigDecimal("82000"), LocalDate.of(2022, 5, 1), finance);
        Employee empAlice = createEmployee(emp1, "Alice", "Johnson", "Senior Developer",
                new BigDecimal("72000"), LocalDate.of(2023, 1, 10), frontend);
        Employee empBob = createEmployee(emp2, "Bob", "Wilson", "Backend Developer",
                new BigDecimal("68000"), LocalDate.of(2023, 4, 20), backend);
        Employee empClara = createEmployee(emp3, "Clara", "Müller", "HR Specialist",
                new BigDecimal("55000"), LocalDate.of(2023, 6, 1), hr);
        Employee empDavid = createEmployee(emp4, "David", "Schmidt", "Accountant",
                new BigDecimal("60000"), LocalDate.of(2023, 8, 15), finance);
        Employee empEmma = createEmployee(emp5, "Emma", "Fischer", "Frontend Developer",
                new BigDecimal("65000"), LocalDate.of(2024, 1, 8), frontend);
        Employee empFrank = createEmployee(emp6, "Frank", "Weber", "DevOps Engineer",
                new BigDecimal("70000"), LocalDate.of(2024, 3, 1), engineering);

        // ── Leave Requests ─────────────────────────────────
        createLeave(empAlice, LeaveType.ANNUAL, LocalDate.of(2026, 4, 7),
                LocalDate.of(2026, 4, 11), LeaveStatus.APPROVED, "Spring vacation");
        createLeave(empBob, LeaveType.SICK, LocalDate.of(2026, 3, 24),
                LocalDate.of(2026, 3, 25), LeaveStatus.APPROVED, "Flu");
        createLeave(empClara, LeaveType.ANNUAL, LocalDate.of(2026, 5, 12),
                LocalDate.of(2026, 5, 23), LeaveStatus.PENDING, "Summer holiday");
        createLeave(empDavid, LeaveType.UNPAID, LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 5), LeaveStatus.PENDING, "Personal matters");
        createLeave(empEmma, LeaveType.ANNUAL, LocalDate.of(2026, 4, 21),
                LocalDate.of(2026, 4, 25), LeaveStatus.REJECTED, "Overlaps with sprint deadline");

        // ── Payroll Records (March 2026) ───────────────────
        createPayroll(empJohn, 2026, 3, new BigDecimal("85000"), new BigDecimal("5000"),
                new BigDecimal("22500"), PayrollStatus.PAID);
        createPayroll(empJane, 2026, 3, new BigDecimal("82000"), new BigDecimal("4000"),
                new BigDecimal("21500"), PayrollStatus.PAID);
        createPayroll(empAlice, 2026, 3, new BigDecimal("72000"), new BigDecimal("3000"),
                new BigDecimal("18750"), PayrollStatus.PAID);
        createPayroll(empBob, 2026, 3, new BigDecimal("68000"), new BigDecimal("2000"),
                new BigDecimal("17500"), PayrollStatus.DRAFT);
        createPayroll(empClara, 2026, 3, new BigDecimal("55000"), BigDecimal.ZERO,
                new BigDecimal("13750"), PayrollStatus.DRAFT);
        createPayroll(empDavid, 2026, 3, new BigDecimal("60000"), new BigDecimal("1500"),
                new BigDecimal("15375"), PayrollStatus.PAID);

        log.info("Seed data loaded: {} users, {} departments, {} employees, {} leaves, {} payrolls",
                userRepository.count(), departmentRepository.count(), employeeRepository.count(),
                leaveRequestRepository.count(), payrollRepository.count());
    }

    private User createUser(String email, String password, Role role) {
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .isActive(true)
                .preferredLang("en")
                .build();
        return userRepository.save(user);
    }

    private Department createDepartment(String name, User manager, Department parent) {
        Department dept = Department.builder()
                .name(name)
                .manager(manager)
                .parentDepartment(parent)
                .build();
        return departmentRepository.save(dept);
    }

    private Employee createEmployee(User user, String firstName, String lastName,
                                     String position, BigDecimal salary,
                                     LocalDate hireDate, Department department) {
        Employee emp = Employee.builder()
                .user(user)
                .firstName(firstName)
                .lastName(lastName)
                .position(position)
                .salary(salary)
                .hireDate(hireDate)
                .department(department)
                .build();
        return employeeRepository.save(emp);
    }

    private void createLeave(Employee employee, LeaveType type,
                              LocalDate start, LocalDate end,
                              LeaveStatus status, String reason) {
        LeaveRequest leave = LeaveRequest.builder()
                .employee(employee)
                .type(type)
                .startDate(start)
                .endDate(end)
                .status(status)
                .reason(reason)
                .build();
        leaveRequestRepository.save(leave);
    }

    private void createPayroll(Employee employee, int year, int month,
                                BigDecimal baseSalary, BigDecimal bonus,
                                BigDecimal deductions, PayrollStatus status) {
        BigDecimal monthly = baseSalary.divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal net = monthly.add(bonus).subtract(deductions);

        PayrollRecord payroll = PayrollRecord.builder()
                .employee(employee)
                .year(year)
                .month(month)
                .baseSalary(monthly)
                .bonus(bonus)
                .deductions(deductions)
                .netSalary(net)
                .status(status)
                .build();
        payrollRepository.save(payroll);
    }
}
