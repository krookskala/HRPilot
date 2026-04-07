package com.hrpilot.backend.seed;

import com.hrpilot.backend.department.Department;
import com.hrpilot.backend.department.DepartmentRepository;
import com.hrpilot.backend.employee.Employee;
import com.hrpilot.backend.employee.EmployeeRepository;
import com.hrpilot.backend.employee.EmploymentHistory;
import com.hrpilot.backend.employee.EmploymentHistoryRepository;
import com.hrpilot.backend.leave.LeaveBalance;
import com.hrpilot.backend.leave.LeaveBalanceRepository;
import com.hrpilot.backend.leave.LeaveCalendarService;
import com.hrpilot.backend.leave.LeaveRequest;
import com.hrpilot.backend.leave.LeaveRequestHistoryRepository;
import com.hrpilot.backend.leave.LeaveRequestRepository;
import com.hrpilot.backend.payroll.PayrollCalculatorService;
import com.hrpilot.backend.payroll.PayrollComponentRepository;
import com.hrpilot.backend.payroll.PayrollRepository;
import com.hrpilot.backend.payroll.PayrollRunRepository;
import com.hrpilot.backend.user.Role;
import com.hrpilot.backend.user.User;
import com.hrpilot.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder {

    private static final String COMPANY_NAME = "Novacore Systems GmbH";
    private static final String COMPANY_DOMAIN = "novacore-systems.de";
    private static final String EMPLOYEE_PASSWORD = "demo1234";
    private static final int TARGET_EMPLOYEE_COUNT = 100;
    private static final int RANDOM_SEED = 20260407;
    private static final int ANNUAL_LEAVE_DAYS = 30;
    private static final int SICK_LEAVE_DAYS = 15;
    private static final int UNPAID_LEAVE_DAYS = 10;

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final EmploymentHistoryRepository employmentHistoryRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveRequestHistoryRepository leaveRequestHistoryRepository;
    private final PayrollRepository payrollRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final PayrollComponentRepository payrollComponentRepository;
    private final PasswordEncoder passwordEncoder;
    private final LeaveCalendarService leaveCalendarService;
    private final PayrollCalculatorService payrollCalculatorService;

    private final Random random = new Random(RANDOM_SEED);
    private final Set<String> usedFullNames = new HashSet<>();
    private final Map<String, Integer> emailSuffixes = new HashMap<>();
    private final List<String> malePhotoAssets = loadAssetUrls("man");
    private final List<String> femalePhotoAssets = loadAssetUrls("woman");
    private int malePhotoIndex = 0;
    private int femalePhotoIndex = 0;

    public void seedIfEmpty() {
        if (userRepository.count() > 0) {
            backfillEmployeePhotosIfMissing();
            log.info("Database already has data - skipping full seed.");
            return;
        }

        log.info("Seeding {} demo company data...", COMPANY_NAME);

        createUser("admin@hrpilot.com", "admin123", Role.ADMIN);

        List<DepartmentBlueprint> blueprints = DemoCompanyBlueprints.departments();
        Map<String, EmployeeSeedContext> managersByDepartment = new LinkedHashMap<>();
        List<EmployeeSeedContext> employees = new ArrayList<>();

        for (DepartmentBlueprint blueprint : blueprints) {
            usedFullNames.add(blueprint.managerFirstName() + " " + blueprint.managerLastName());
            User managerUser = createCompanyUser(blueprint.managerFirstName(), blueprint.managerLastName(), blueprint.managerRole());
            Department parentDepartment = blueprint.parentKey() == null
                ? null
                : managersByDepartment.get(blueprint.parentKey()).department();
            Department department = createDepartment(blueprint.name(), managerUser, parentDepartment);

            EmployeeSeedContext managerContext = createEmployeeSeed(
                managerUser,
                department,
                blueprint,
                blueprint.managerFirstName(),
                blueprint.managerLastName(),
                blueprint.managerProfile(),
                randomManagerHireDate(),
                chooseTaxClass(),
                true
            );
            managersByDepartment.put(blueprint.key(), managerContext);
            employees.add(managerContext);
        }

        int contributorsToCreate = TARGET_EMPLOYEE_COUNT - employees.size();
        List<PersonName> contributorNames = generateUniqueNames(contributorsToCreate);
        int index = 0;

        for (DepartmentBlueprint blueprint : blueprints) {
            Department department = managersByDepartment.get(blueprint.key()).department();
            for (int i = 0; i < blueprint.headcount() - 1; i++) {
                PersonName name = contributorNames.get(index++);
                PositionProfile position = randomFrom(blueprint.contributorProfiles());
                User user = createCompanyUser(name.firstName(), name.lastName(), Role.EMPLOYEE);
                employees.add(createEmployeeSeed(
                    user,
                    department,
                    blueprint,
                    name.firstName(),
                    name.lastName(),
                    position,
                    randomContributorHireDate(),
                    chooseTaxClass(),
                    false
                ));
            }
        }

        createEmploymentHistory(employees);
        List<LeaveRequest> leaveRequests = createLeaveRequests(employees, managersByDepartment, LocalDate.now().getYear());
        createLeaveBalances(employees, leaveRequests, LocalDate.now().getYear());
        createPayrollRuns(employees, managersByDepartment.get("people-ops").user());

        log.info(
            "Seed data loaded: {} users, {} departments, {} employees, {} leave balances, {} leave requests, {} payroll records",
            userRepository.count(),
            departmentRepository.count(),
            employeeRepository.count(),
            leaveBalanceRepository.count(),
            leaveRequestRepository.count(),
            payrollRepository.count()
        );
        log.info("Seed logins: admin@hrpilot.com / admin123, lena.hoffmann@{} / {}", COMPANY_DOMAIN, EMPLOYEE_PASSWORD);
        log.info("Department manager login: marco.weber@{} / {}", COMPANY_DOMAIN, EMPLOYEE_PASSWORD);
        log.info("All seeded employee accounts use password: {}", EMPLOYEE_PASSWORD);
    }

    private void backfillEmployeePhotosIfMissing() {
        List<Employee> employeesMissingPhotos = employeeRepository.findAll().stream()
            .filter(employee -> employee.getPhotoUrl() == null || employee.getPhotoUrl().isBlank())
            .sorted(java.util.Comparator.comparing(Employee::getId))
            .toList();

        if (employeesMissingPhotos.isEmpty()) {
            return;
        }

        int updatedCount = 0;
        for (Employee employee : employeesMissingPhotos) {
            String photoUrl = nextSeedPhotoUrl(employee.getFirstName());
            if (photoUrl == null) {
                continue;
            }
            employee.setPhotoUrl(photoUrl);
            employeeRepository.save(employee);
            updatedCount++;
        }

        log.info("Backfilled photo assets for {} existing employees.", updatedCount);
    }

    private EmployeeSeedContext createEmployeeSeed(
        User user,
        Department department,
        DepartmentBlueprint blueprint,
        String firstName,
        String lastName,
        PositionProfile position,
        LocalDate hireDate,
        String taxClass,
        boolean manager
    ) {
        Employee employee = employeeRepository.save(Employee.builder()
            .user(user)
            .firstName(firstName)
            .lastName(lastName)
            .position(position.title())
            .salary(randomSalary(position.minSalary(), position.maxSalary()))
            .hireDate(hireDate)
            .department(department)
            .photoUrl(nextSeedPhotoUrl(firstName))
            .phone(randomPhoneNumber())
            .address(randomAddress())
            .emergencyContactName(randomEmergencyContact(firstName, lastName))
            .emergencyContactPhone(randomPhoneNumber())
            .build());

        return new EmployeeSeedContext(employee, user, department, blueprint, taxClass, manager);
    }

    private void createEmploymentHistory(List<EmployeeSeedContext> employees) {
        List<EmploymentHistory> historyEntries = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (EmployeeSeedContext context : employees) {
            Employee employee = context.employee();
            if (employee.getHireDate().isBefore(today.minusYears(2)) && random.nextDouble() < 0.72) {
                BigDecimal newSalary = employee.getSalary();
                BigDecimal oldSalary = newSalary
                    .divide(BigDecimal.valueOf(1.08 + (random.nextDouble() * 0.06)), 0, RoundingMode.HALF_UP)
                    .setScale(0, RoundingMode.HALF_UP);

                historyEntries.add(EmploymentHistory.builder()
                    .employee(employee)
                    .changeType("SALARY")
                    .oldValue(formatMoney(oldSalary))
                    .newValue(formatMoney(newSalary))
                    .changedAt(randomDateTimeBetween(
                        employee.getHireDate().plusMonths(8).atStartOfDay(),
                        today.minusMonths(2).atTime(17, 0)
                    ))
                    .build());
            }

            String previousTitle = previousPositionFor(employee.getPosition());
            if (previousTitle != null && employee.getHireDate().isBefore(today.minusYears(3)) && random.nextDouble() < 0.38) {
                historyEntries.add(EmploymentHistory.builder()
                    .employee(employee)
                    .changeType("POSITION")
                    .oldValue(previousTitle)
                    .newValue(employee.getPosition())
                    .changedAt(randomDateTimeBetween(
                        employee.getHireDate().plusMonths(12).atStartOfDay(),
                        today.minusMonths(4).atTime(16, 30)
                    ))
                    .build());
            }
        }

        employmentHistoryRepository.saveAll(historyEntries);
    }

    private void createLeaveBalances(List<EmployeeSeedContext> employees, List<LeaveRequest> leaveRequests, int year) {
        Map<Long, Map<com.hrpilot.backend.leave.LeaveType, Integer>> approvedDays = new HashMap<>();

        for (LeaveRequest leaveRequest : leaveRequests) {
            if (leaveRequest.getStatus() != com.hrpilot.backend.leave.LeaveStatus.APPROVED) {
                continue;
            }

            Map<com.hrpilot.backend.leave.LeaveType, Integer> employeeUsage = approvedDays.computeIfAbsent(
                leaveRequest.getEmployee().getId(),
                ignored -> new EnumMap<>(com.hrpilot.backend.leave.LeaveType.class)
            );
            employeeUsage.merge(leaveRequest.getType(), leaveRequest.getWorkingDays(), Integer::sum);
        }

        List<LeaveBalance> balances = new ArrayList<>();
        for (EmployeeSeedContext context : employees) {
            Map<com.hrpilot.backend.leave.LeaveType, Integer> usage = approvedDays.getOrDefault(context.employee().getId(), Map.of());
            int annualTotal = context.manager() ? 32 : ANNUAL_LEAVE_DAYS;
            balances.add(buildLeaveBalance(context.employee(), com.hrpilot.backend.leave.LeaveType.ANNUAL, year, annualTotal, usage.getOrDefault(com.hrpilot.backend.leave.LeaveType.ANNUAL, 0)));
            balances.add(buildLeaveBalance(context.employee(), com.hrpilot.backend.leave.LeaveType.SICK, year, SICK_LEAVE_DAYS, usage.getOrDefault(com.hrpilot.backend.leave.LeaveType.SICK, 0)));
            balances.add(buildLeaveBalance(context.employee(), com.hrpilot.backend.leave.LeaveType.UNPAID, year, UNPAID_LEAVE_DAYS, usage.getOrDefault(com.hrpilot.backend.leave.LeaveType.UNPAID, 0)));
        }

        leaveBalanceRepository.saveAll(balances);
    }

    private LeaveBalance buildLeaveBalance(Employee employee, com.hrpilot.backend.leave.LeaveType leaveType, int year, int totalDays, int usedDays) {
        return LeaveBalance.builder()
            .employee(employee)
            .leaveType(leaveType)
            .year(year)
            .totalDays(totalDays)
            .usedDays(Math.min(totalDays, usedDays))
            .build();
    }

    private User createCompanyUser(String firstName, String lastName, Role role) {
        return createUser(nextCompanyEmail(firstName, lastName), EMPLOYEE_PASSWORD, role);
    }

    private User createUser(String email, String password, Role role) {
        return userRepository.save(User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(password))
            .role(role)
            .active(true)
            .preferredLang("en")
            .darkMode(false)
            .activatedAt(LocalDateTime.now())
            .build());
    }

    private Department createDepartment(String name, User manager, Department parentDepartment) {
        return departmentRepository.save(Department.builder()
            .name(name)
            .manager(manager)
            .parentDepartment(parentDepartment)
            .build());
    }

    private List<LeaveRequest> createLeaveRequests(
        List<EmployeeSeedContext> employees,
        Map<String, EmployeeSeedContext> managersByDepartment,
        int year
    ) {
        List<EmployeeSeedContext> shuffledEmployees = new ArrayList<>(employees);
        java.util.Collections.shuffle(shuffledEmployees, random);

        int targetEmployeesWithLeave = Math.min(58, shuffledEmployees.size());
        List<LeaveRequest> leaveRequests = new ArrayList<>();
        List<com.hrpilot.backend.leave.LeaveRequestHistory> historyEntries = new ArrayList<>();
        Map<Long, List<DateWindow>> reservedByEmployee = new HashMap<>();
        EmployeeSeedContext hrLead = managersByDepartment.get("people-ops");

        for (int i = 0; i < targetEmployeesWithLeave; i++) {
            EmployeeSeedContext context = shuffledEmployees.get(i);
            int requestCount = random.nextDouble() < 0.22 ? 2 : 1;
            for (int j = 0; j < requestCount; j++) {
                LeaveScenario scenario = buildLeaveScenario(context, reservedByEmployee, managersByDepartment, hrLead, year);
                if (scenario == null) {
                    continue;
                }
                LeaveRequest savedLeave = leaveRequestRepository.save(scenario.leaveRequest());
                leaveRequests.add(savedLeave);
                historyEntries.addAll(rebindHistory(savedLeave, scenario.historyEntries()));
            }
        }

        leaveRequestHistoryRepository.saveAll(historyEntries);
        return leaveRequests;
    }

    private List<com.hrpilot.backend.leave.LeaveRequestHistory> rebindHistory(
        LeaveRequest savedLeave,
        Collection<com.hrpilot.backend.leave.LeaveRequestHistory> historyEntries
    ) {
        return historyEntries.stream()
            .map(entry -> com.hrpilot.backend.leave.LeaveRequestHistory.builder()
                .leaveRequest(savedLeave)
                .actorUser(entry.getActorUser())
                .actionType(entry.getActionType())
                .fromStatus(entry.getFromStatus())
                .toStatus(entry.getToStatus())
                .note(entry.getNote())
                .occurredAt(entry.getOccurredAt())
                .build())
            .toList();
    }

    private LeaveScenario buildLeaveScenario(
        EmployeeSeedContext context,
        Map<Long, List<DateWindow>> reservedByEmployee,
        Map<String, EmployeeSeedContext> managersByDepartment,
        EmployeeSeedContext hrLead,
        int year
    ) {
        com.hrpilot.backend.leave.LeaveType leaveType = weightedLeaveType();
        com.hrpilot.backend.leave.LeaveStatus status = weightedLeaveStatus();
        LocalDate startDate = chooseLeaveStart(status, year);
        LocalDate endDate = endDateForWorkingDays(startDate, targetWorkingDays(leaveType));
        if (endDate == null) {
            return null;
        }

        List<DateWindow> reserved = reservedByEmployee.computeIfAbsent(context.employee().getId(), ignored -> new ArrayList<>());
        for (DateWindow window : reserved) {
            if (window.overlaps(startDate, endDate)) {
                return null;
            }
        }
        reserved.add(new DateWindow(startDate, endDate));

        int workingDays = leaveCalendarService.calculateWorkingDays(startDate, endDate);
        if (workingDays <= 0) {
            return null;
        }

        User approver = resolveLeaveApprover(context, managersByDepartment, hrLead);
        String requestReason = requestReasonFor(leaveType);
        LocalDateTime createdAt = randomDateTimeBetween(
            startDate.minusDays(55).atTime(9, 0),
            startDate.minusDays(4).atTime(16, 0)
        );
        LocalDateTime actionedAt = status == com.hrpilot.backend.leave.LeaveStatus.PENDING
            ? null
            : createdAt.plusDays(1 + random.nextInt(5)).withHour(11 + random.nextInt(5)).withMinute(random.nextInt(60));

        LeaveRequest leaveRequest = LeaveRequest.builder()
            .employee(context.employee())
            .type(leaveType)
            .startDate(startDate)
            .endDate(endDate)
            .workingDays(workingDays)
            .status(status)
            .reason(requestReason)
            .approvedByUser(status == com.hrpilot.backend.leave.LeaveStatus.APPROVED ? approver : null)
            .rejectedByUser(status == com.hrpilot.backend.leave.LeaveStatus.REJECTED ? approver : null)
            .cancelledByUser(status == com.hrpilot.backend.leave.LeaveStatus.CANCELLED ? context.user() : null)
            .actionedAt(actionedAt)
            .cancelledAt(status == com.hrpilot.backend.leave.LeaveStatus.CANCELLED ? actionedAt : null)
            .rejectionReason(status == com.hrpilot.backend.leave.LeaveStatus.REJECTED ? rejectionReasonFor(context.department().getName()) : null)
            .cancellationReason(status == com.hrpilot.backend.leave.LeaveStatus.CANCELLED ? cancellationReasonFor() : null)
            .build();

        List<com.hrpilot.backend.leave.LeaveRequestHistory> historyEntries = new ArrayList<>();
        historyEntries.add(com.hrpilot.backend.leave.LeaveRequestHistory.builder()
            .leaveRequest(leaveRequest)
            .actorUser(context.user())
            .actionType(com.hrpilot.backend.leave.LeaveActionType.CREATED)
            .fromStatus(null)
            .toStatus(com.hrpilot.backend.leave.LeaveStatus.PENDING)
            .note(requestReason)
            .occurredAt(createdAt)
            .build());

        if (status == com.hrpilot.backend.leave.LeaveStatus.APPROVED) {
            historyEntries.add(com.hrpilot.backend.leave.LeaveRequestHistory.builder()
                .leaveRequest(leaveRequest)
                .actorUser(approver)
                .actionType(com.hrpilot.backend.leave.LeaveActionType.APPROVED)
                .fromStatus(com.hrpilot.backend.leave.LeaveStatus.PENDING)
                .toStatus(com.hrpilot.backend.leave.LeaveStatus.APPROVED)
                .note("Approved in the regular manager review cycle.")
                .occurredAt(actionedAt)
                .build());
        } else if (status == com.hrpilot.backend.leave.LeaveStatus.REJECTED) {
            historyEntries.add(com.hrpilot.backend.leave.LeaveRequestHistory.builder()
                .leaveRequest(leaveRequest)
                .actorUser(approver)
                .actionType(com.hrpilot.backend.leave.LeaveActionType.REJECTED)
                .fromStatus(com.hrpilot.backend.leave.LeaveStatus.PENDING)
                .toStatus(com.hrpilot.backend.leave.LeaveStatus.REJECTED)
                .note(leaveRequest.getRejectionReason())
                .occurredAt(actionedAt)
                .build());
        } else if (status == com.hrpilot.backend.leave.LeaveStatus.CANCELLED) {
            historyEntries.add(com.hrpilot.backend.leave.LeaveRequestHistory.builder()
                .leaveRequest(leaveRequest)
                .actorUser(context.user())
                .actionType(com.hrpilot.backend.leave.LeaveActionType.CANCELLED)
                .fromStatus(com.hrpilot.backend.leave.LeaveStatus.PENDING)
                .toStatus(com.hrpilot.backend.leave.LeaveStatus.CANCELLED)
                .note(leaveRequest.getCancellationReason())
                .occurredAt(actionedAt)
                .build());
        }

        return new LeaveScenario(leaveRequest, historyEntries);
    }

    private void createPayrollRuns(List<EmployeeSeedContext> employees, User generatedByUser) {
        List<java.time.YearMonth> periods = List.of(
            java.time.YearMonth.from(LocalDate.now()).minusMonths(3),
            java.time.YearMonth.from(LocalDate.now()).minusMonths(2),
            java.time.YearMonth.from(LocalDate.now()).minusMonths(1),
            java.time.YearMonth.from(LocalDate.now())
        );
        List<com.hrpilot.backend.payroll.PayrollRunStatus> statuses = List.of(
            com.hrpilot.backend.payroll.PayrollRunStatus.PAID,
            com.hrpilot.backend.payroll.PayrollRunStatus.PAID,
            com.hrpilot.backend.payroll.PayrollRunStatus.PUBLISHED,
            com.hrpilot.backend.payroll.PayrollRunStatus.DRAFT
        );

        for (int i = 0; i < periods.size(); i++) {
            java.time.YearMonth period = periods.get(i);
            com.hrpilot.backend.payroll.PayrollRunStatus status = statuses.get(i);
            com.hrpilot.backend.payroll.PayrollRun run = payrollRunRepository.save(com.hrpilot.backend.payroll.PayrollRun.builder()
                .name(period.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH) + " " + period.getYear() + " Payroll")
                .year(period.getYear())
                .month(period.getMonthValue())
                .status(status)
                .generatedByUser(generatedByUser)
                .publishedAt(status != com.hrpilot.backend.payroll.PayrollRunStatus.DRAFT ? period.atEndOfMonth().atTime(17, 30) : null)
                .paidAt(status == com.hrpilot.backend.payroll.PayrollRunStatus.PAID ? period.atEndOfMonth().plusDays(1).atTime(10, 0) : null)
                .build());

            for (EmployeeSeedContext context : employees) {
                if (context.employee().getHireDate().isAfter(period.atEndOfMonth())) {
                    continue;
                }
                createPayrollRecord(context, period, status, run);
            }
        }
    }

    private void createPayrollRecord(
        EmployeeSeedContext context,
        java.time.YearMonth period,
        com.hrpilot.backend.payroll.PayrollRunStatus status,
        com.hrpilot.backend.payroll.PayrollRun run
    ) {
        BigDecimal bonus = payrollBonusFor(context, period);
        BigDecimal additionalDeduction = payrollDeductionFor(context, period);
        com.hrpilot.backend.payroll.PayrollCalculationResult calculation = payrollCalculatorService.calculate(
            context.employee(),
            bonus,
            additionalDeduction,
            context.taxClass()
        );

        com.hrpilot.backend.payroll.PayrollRecord record = payrollRepository.save(com.hrpilot.backend.payroll.PayrollRecord.builder()
            .employee(context.employee())
            .run(run)
            .year(period.getYear())
            .month(period.getMonthValue())
            .baseSalary(calculation.monthlyBaseSalary())
            .grossSalary(calculation.grossSalary())
            .bonus(bonus)
            .deductions(calculation.totalDeductions())
            .employeeSocialContributions(calculation.employeeSocialContributions())
            .employerSocialContributions(calculation.employerSocialContributions())
            .incomeTax(calculation.incomeTax())
            .taxClass(calculation.taxClass())
            .netSalary(calculation.netSalary())
            .status(toPayrollStatus(status))
            .publishedAt(status != com.hrpilot.backend.payroll.PayrollRunStatus.DRAFT ? run.getPublishedAt() : null)
            .paidAt(status == com.hrpilot.backend.payroll.PayrollRunStatus.PAID ? run.getPaidAt() : null)
            .build());

        List<com.hrpilot.backend.payroll.PayrollComponent> components = calculation.components().stream()
            .map(component -> com.hrpilot.backend.payroll.PayrollComponent.builder()
                .payrollRecord(record)
                .componentType(component.componentType())
                .code(component.code())
                .label(component.label())
                .amount(component.amount())
                .build())
            .toList();
        payrollComponentRepository.saveAll(components);
    }

    private com.hrpilot.backend.payroll.PayrollStatus toPayrollStatus(com.hrpilot.backend.payroll.PayrollRunStatus status) {
        return switch (status) {
            case DRAFT -> com.hrpilot.backend.payroll.PayrollStatus.DRAFT;
            case PUBLISHED -> com.hrpilot.backend.payroll.PayrollStatus.PUBLISHED;
            case PAID -> com.hrpilot.backend.payroll.PayrollStatus.PAID;
        };
    }

    private BigDecimal payrollBonusFor(EmployeeSeedContext context, java.time.YearMonth period) {
        String departmentKey = context.blueprint().key();
        String position = context.employee().getPosition();

        if ("sales".equals(departmentKey)) {
            return amountBetween(350, 2200);
        }
        if ("customer-success".equals(departmentKey) && random.nextDouble() < 0.38) {
            return amountBetween(150, 700);
        }
        if ("marketing".equals(departmentKey) && random.nextDouble() < 0.25) {
            return amountBetween(120, 500);
        }
        if (position.contains("Director") || position.contains("Head of")) {
            return period.getMonthValue() % 3 == 0 ? amountBetween(1200, 2600) : amountBetween(300, 900);
        }
        if (position.contains("Manager") && random.nextDouble() < 0.45) {
            return amountBetween(400, 1200);
        }
        if (position.contains("Engineer") && random.nextDouble() < 0.18) {
            return amountBetween(120, 480);
        }
        if (random.nextDouble() < 0.08) {
            return amountBetween(100, 300);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal payrollDeductionFor(EmployeeSeedContext context, java.time.YearMonth period) {
        if (period.equals(java.time.YearMonth.from(LocalDate.now())) && random.nextDouble() < 0.12) {
            return amountBetween(80, 220);
        }
        if ("sales".equals(context.blueprint().key()) && random.nextDouble() < 0.15) {
            return amountBetween(60, 180);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private String nextCompanyEmail(String firstName, String lastName) {
        String localPart = slug(firstName) + "." + slug(lastName);
        int occurrence = emailSuffixes.merge(localPart, 1, Integer::sum);
        String finalLocalPart = occurrence == 1 ? localPart : localPart + occurrence;
        return finalLocalPart + "@" + COMPANY_DOMAIN;
    }

    private String slug(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    private List<PersonName> generateUniqueNames(int targetCount) {
        String[] firstNames = {
            "Anna", "Leon", "Mia", "Paul", "Emma", "Noah", "Lina", "Elias", "Leonie", "Finn",
            "Clara", "Julian", "Sarah", "Lukas", "Julia", "David", "Elena", "Max", "Hannah", "Felix",
            "Marlene", "Niklas", "Paula", "Simon", "Ida", "Adrian", "Theresa", "Philipp", "Amelie", "Robin",
            "Franziska", "Sebastian", "Charlotte", "Matthias", "Kathrin", "Alexander", "Johanna", "Moritz", "Miriam", "Tim",
            "Vanessa", "Patrick", "Nora", "Kevin", "Luisa", "Jan", "Lisa", "Dominik", "Aylin", "Fabian",
            "Selina", "Bastian", "Mara", "Vincent", "Daniela", "Christian", "Helena", "Tom", "Tina", "Oliver"
        };
        String[] lastNames = {
            "Mueller", "Schneider", "Fischer", "Meyer", "Wagner", "Becker", "Schulz", "Hoffmann", "Schaefer", "Koch",
            "Bauer", "Richter", "Klein", "Wolf", "Schroeder", "Neumann", "Schwarz", "Zimmermann", "Braun", "Krueger",
            "Hartmann", "Lange", "Schmitt", "Werner", "Krause", "Meier", "Lehmann", "Schmid", "Kraemer", "Voigt",
            "Peters", "Arnold", "Frank", "Busch", "Kaiser", "Winter", "Otto", "Seidel", "Lorenz", "Reuter",
            "Weiss", "Pohl", "Jung", "Kuhn", "Graf", "Ulrich", "Horn", "Vogel", "Binder", "Roth"
        };

        List<PersonName> names = new ArrayList<>();
        while (names.size() < targetCount) {
            PersonName candidate = new PersonName(randomFrom(firstNames), randomFrom(lastNames));
            String fullName = candidate.firstName() + " " + candidate.lastName();
            if (usedFullNames.add(fullName)) {
                names.add(candidate);
            }
        }
        return names;
    }

    private LocalDate randomManagerHireDate() {
        return randomDateBetween(LocalDate.of(2018, 1, 15), LocalDate.now().minusYears(2));
    }

    private LocalDate randomContributorHireDate() {
        return randomDateBetween(LocalDate.of(2019, 2, 1), LocalDate.now().minusWeeks(3));
    }

    private LocalDate randomDateBetween(LocalDate startInclusive, LocalDate endInclusive) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(startInclusive, endInclusive);
        if (days <= 0) {
            return startInclusive;
        }
        return startInclusive.plusDays(random.nextLong(days + 1));
    }

    private LocalDateTime randomDateTimeBetween(LocalDateTime startInclusive, LocalDateTime endInclusive) {
        long seconds = java.time.Duration.between(startInclusive, endInclusive).getSeconds();
        if (seconds <= 0) {
            return startInclusive;
        }
        return startInclusive.plusSeconds(random.nextLong(seconds + 1));
    }

    private String chooseTaxClass() {
        int roll = random.nextInt(100);
        if (roll < 56) {
            return "I";
        }
        if (roll < 67) {
            return "IV";
        }
        if (roll < 79) {
            return "III";
        }
        if (roll < 90) {
            return "II";
        }
        return "V";
    }

    private BigDecimal randomSalary(int min, int max) {
        int steps = ((max - min) / 500) + 1;
        int salary = min + (random.nextInt(steps) * 500);
        return BigDecimal.valueOf(salary).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal amountBetween(int min, int max) {
        int steps = ((max - min) / 25) + 1;
        int amount = min + (random.nextInt(steps) * 25);
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private String nextSeedPhotoUrl(String firstName) {
        if (isFemaleFirstName(firstName)) {
            return nextPhotoFromPool(femalePhotoAssets, true);
        }
        return nextPhotoFromPool(malePhotoAssets, false);
    }

    private String nextPhotoFromPool(List<String> pool, boolean female) {
        if (pool.isEmpty()) {
            return null;
        }

        if (female) {
            String asset = pool.get(femalePhotoIndex % pool.size());
            femalePhotoIndex++;
            return asset;
        }

        String asset = pool.get(malePhotoIndex % pool.size());
        malePhotoIndex++;
        return asset;
    }

    private boolean isFemaleFirstName(String firstName) {
        return Set.of(
            "Anna", "Mia", "Emma", "Lina", "Leonie", "Clara", "Sarah", "Julia", "Elena", "Hannah",
            "Marlene", "Paula", "Ida", "Theresa", "Amelie", "Franziska", "Charlotte", "Kathrin",
            "Johanna", "Miriam", "Vanessa", "Nora", "Luisa", "Lisa", "Aylin", "Selina", "Mara",
            "Daniela", "Helena", "Tina", "Nina", "Laura", "Sophie", "Lena", "Katharina"
        ).contains(firstName);
    }

    private List<String> loadAssetUrls(String folderName) {
        Path basePath = resolveFrontendAssetsPath(folderName);
        if (basePath == null) {
            log.warn("Frontend asset folder for '{}' photos was not found. Seeded employees will fall back to initials.", folderName);
            return List.of();
        }

        try (Stream<Path> paths = Files.list(basePath)) {
            return paths
                .filter(Files::isRegularFile)
                .map(path -> "/assets/" + folderName + "/" + path.getFileName())
                .sorted()
                .toList();
        } catch (IOException exception) {
            log.warn("Failed to read photo assets from {}", basePath, exception);
            return List.of();
        }
    }

    private Path resolveFrontendAssetsPath(String folderName) {
        Path[] candidates = {
            Path.of("frontend", "assets", folderName),
            Path.of("..", "frontend", "assets", folderName)
        };

        for (Path candidate : candidates) {
            Path absolute = candidate.toAbsolutePath().normalize();
            if (Files.isDirectory(absolute)) {
                return absolute;
            }
        }

        return null;
    }

    private String randomPhoneNumber() {
        return String.format(Locale.ROOT, "+49 15%d %03d %04d", 1 + random.nextInt(8), random.nextInt(1000), random.nextInt(10000));
    }

    private String randomAddress() {
        String[] streets = {
            "Koenigsallee", "Lindenstrasse", "Bergmannstrasse", "Rosenweg", "Hafenstrasse",
            "Goethestrasse", "Parkallee", "Schillerstrasse", "Bahnhofstrasse", "Sonnenweg",
            "Gartenstrasse", "Muehlenweg", "Alte Landstrasse", "Rheinweg", "Friedrichstrasse"
        };
        String[] cities = {
            "Berlin", "Hamburg", "Munich", "Cologne", "Frankfurt", "Stuttgart", "Duesseldorf",
            "Leipzig", "Dresden", "Bremen", "Hanover", "Bonn", "Nuremberg", "Mannheim"
        };

        String street = randomFrom(streets);
        String city = randomFrom(cities);
        int houseNumber = 3 + random.nextInt(115);
        int postcode = 10000 + random.nextInt(79999);
        return street + " " + houseNumber + ", " + postcode + " " + city + ", Germany";
    }

    private String randomEmergencyContact(String employeeFirstName, String employeeLastName) {
        List<PersonName> emergencyContacts = List.of(
            new PersonName("Martin", employeeLastName),
            new PersonName("Sabine", employeeLastName),
            new PersonName("Petra", employeeLastName),
            new PersonName("Thomas", employeeLastName),
            new PersonName("Julia", employeeLastName),
            new PersonName("Andreas", employeeLastName)
        );
        PersonName name = randomFrom(emergencyContacts);
        if (name.firstName().equalsIgnoreCase(employeeFirstName)) {
            return "Elena " + employeeLastName;
        }
        return name.firstName() + " " + name.lastName();
    }

    private com.hrpilot.backend.leave.LeaveType weightedLeaveType() {
        int roll = random.nextInt(100);
        if (roll < 63) {
            return com.hrpilot.backend.leave.LeaveType.ANNUAL;
        }
        if (roll < 86) {
            return com.hrpilot.backend.leave.LeaveType.SICK;
        }
        return com.hrpilot.backend.leave.LeaveType.UNPAID;
    }

    private com.hrpilot.backend.leave.LeaveStatus weightedLeaveStatus() {
        int roll = random.nextInt(100);
        if (roll < 59) {
            return com.hrpilot.backend.leave.LeaveStatus.APPROVED;
        }
        if (roll < 80) {
            return com.hrpilot.backend.leave.LeaveStatus.PENDING;
        }
        if (roll < 92) {
            return com.hrpilot.backend.leave.LeaveStatus.REJECTED;
        }
        return com.hrpilot.backend.leave.LeaveStatus.CANCELLED;
    }

    private LocalDate chooseLeaveStart(com.hrpilot.backend.leave.LeaveStatus status, int year) {
        if (status == com.hrpilot.backend.leave.LeaveStatus.PENDING) {
            LocalDate start = LocalDate.now().plusDays(7);
            LocalDate end = LocalDate.of(year, 12, 5);
            return randomDateBetween(start, end.isBefore(start) ? start : end);
        }
        return randomDateBetween(LocalDate.of(year, 1, 6), LocalDate.now().plusWeeks(5));
    }

    private int targetWorkingDays(com.hrpilot.backend.leave.LeaveType leaveType) {
        return switch (leaveType) {
            case ANNUAL -> 3 + random.nextInt(8);
            case SICK -> 1 + random.nextInt(4);
            case UNPAID -> 2 + random.nextInt(5);
        };
    }

    private LocalDate endDateForWorkingDays(LocalDate startDate, int targetWorkingDays) {
        LocalDate endDate = startDate;
        while (leaveCalendarService.calculateWorkingDays(startDate, endDate) < targetWorkingDays) {
            endDate = endDate.plusDays(1);
            if (java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) > 24) {
                return null;
            }
        }
        return endDate;
    }

    private User resolveLeaveApprover(
        EmployeeSeedContext context,
        Map<String, EmployeeSeedContext> managersByDepartment,
        EmployeeSeedContext hrLead
    ) {
        EmployeeSeedContext departmentManager = managersByDepartment.get(context.blueprint().key());
        if (departmentManager == null) {
            return hrLead.user();
        }
        if (departmentManager.user().getId().equals(context.user().getId()) || "people-ops".equals(context.blueprint().key())) {
            return hrLead.user();
        }
        return random.nextDouble() < 0.82 ? departmentManager.user() : hrLead.user();
    }

    private String requestReasonFor(com.hrpilot.backend.leave.LeaveType leaveType) {
        return switch (leaveType) {
            case ANNUAL -> randomFrom(List.of(
                "Planned vacation with family.",
                "Recharge break after product release.",
                "Spring holiday travel already booked.",
                "Family visit outside Germany."
            ));
            case SICK -> randomFrom(List.of(
                "Short-term medical recovery.",
                "Seasonal flu symptoms and doctor recommendation to rest.",
                "Migraine recovery and follow-up appointment.",
                "Minor procedure recovery time."
            ));
            case UNPAID -> randomFrom(List.of(
                "Personal matters requiring a few days away from work.",
                "Home relocation and administrative appointments.",
                "Extended family commitment.",
                "Private travel outside regular annual leave plan."
            ));
        };
    }

    private String rejectionReasonFor(String departmentName) {
        return randomFrom(List.of(
            "Coverage was not sufficient during a key delivery window for " + departmentName + ".",
            "The requested dates overlap with an already approved team absence.",
            "Quarter-end workload and customer commitments require team availability."
        ));
    }

    private String cancellationReasonFor() {
        return randomFrom(List.of(
            "Travel plans changed.",
            "Recovered sooner than expected.",
            "Family plans were rescheduled.",
            "The original request is no longer needed."
        ));
    }

    private String previousPositionFor(String currentPosition) {
        if (currentPosition.startsWith("Senior ")) {
            return currentPosition.replaceFirst("Senior ", "");
        }
        if (currentPosition.startsWith("Staff ")) {
            return currentPosition.replaceFirst("Staff ", "Senior ");
        }
        if (currentPosition.startsWith("Director of ")) {
            return currentPosition.replaceFirst("Director of ", "Senior Manager of ");
        }
        if (currentPosition.startsWith("Director ")) {
            return currentPosition.replaceFirst("Director ", "Senior ");
        }
        if (currentPosition.startsWith("Head of ")) {
            return currentPosition.replaceFirst("Head of ", "Senior ");
        }
        if (currentPosition.endsWith("Manager")) {
            return currentPosition.replace("Manager", "Lead");
        }
        return null;
    }

    private String formatMoney(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP).toPlainString() + " EUR";
    }

    private <T> T randomFrom(List<T> values) {
        return values.get(random.nextInt(values.size()));
    }

    private String randomFrom(String[] values) {
        return values[random.nextInt(values.length)];
    }
}
