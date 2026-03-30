package com.hrpilot.backend.leave;

import com.hrpilot.backend.audit.AuditLogService;
import com.hrpilot.backend.common.exception.BusinessRuleException;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.department.Department;
import com.hrpilot.backend.department.DepartmentScopeService;
import com.hrpilot.backend.employee.Employee;
import com.hrpilot.backend.employee.EmployeeRepository;
import com.hrpilot.backend.leave.dto.CreateLeaveRequest;
import com.hrpilot.backend.leave.dto.LeaveRequestHistoryResponse;
import com.hrpilot.backend.leave.dto.LeaveRequestResponse;
import com.hrpilot.backend.notification.NotificationService;
import com.hrpilot.backend.notification.NotificationType;
import com.hrpilot.backend.user.CurrentUserService;
import com.hrpilot.backend.user.Role;
import com.hrpilot.backend.user.User;
import com.hrpilot.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveRequestService {
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveRequestHistoryRepository leaveRequestHistoryRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentScopeService departmentScopeService;
    private final UserRepository userRepository;
    private final LeaveBalanceService leaveBalanceService;
    private final LeaveCalendarService leaveCalendarService;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Transactional
    public LeaveRequestResponse createLeaveRequest(CreateLeaveRequest request) {
        User actorUser = currentUserService.getCurrentUserEntity();
        log.info("Creating leave request for employee id: {} by user {}", request.employeeId(), actorUser.getEmail());

        Employee employee = employeeRepository.findById(request.employeeId())
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.employeeId()));

        validateDateRange(request.startDate(), request.endDate());
        assertCanCreateLeave(actorUser, employee);
        assertNoOverlap(employee.getId(), request.startDate(), request.endDate());

        int workingDays = leaveCalendarService.calculateWorkingDays(request.startDate(), request.endDate());
        if (workingDays <= 0) {
            throw new BusinessRuleException("Leave request must include at least one working day");
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
            .employee(employee)
            .type(request.type())
            .startDate(request.startDate())
            .endDate(request.endDate())
            .workingDays(workingDays)
            .reason(request.reason())
            .status(LeaveStatus.PENDING)
            .build();

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        recordHistory(saved, actorUser, LeaveActionType.CREATED, null, LeaveStatus.PENDING, request.reason());

        auditLogService.log(
            actorUser,
            "LEAVE_REQUEST_CREATED",
            "LeaveRequest",
            saved.getId().toString(),
            "Leave request submitted for " + employee.getFirstName() + " " + employee.getLastName(),
            request.type() + " " + request.startDate() + " to " + request.endDate()
        );

        notifyApprovers(saved, actorUser);

        log.info("Leave request created successfully with id: {}", saved.getId());
        return toResponse(saved);
    }

    public Page<LeaveRequestResponse> getVisibleLeaveRequests(
            LeaveStatus status,
            LeaveType type,
            Long employeeId,
            Long departmentId,
            Pageable pageable) {
        User actorUser = currentUserService.getCurrentUserEntity();
        Specification<LeaveRequest> specification = buildVisibilitySpecification(actorUser, status, type, employeeId, departmentId);
        return leaveRequestRepository.findAll(specification, pageable).map(this::toResponse);
    }

    public List<LeaveRequestResponse> getLeaveRequestsByEmployee(Long employeeId) {
        User actorUser = currentUserService.getCurrentUserEntity();
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
        assertCanViewLeave(actorUser, employee);
        return leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<LeaveRequestResponse> getCurrentUserLeaveRequests() {
        User actorUser = currentUserService.getCurrentUserEntity();
        return employeeRepository.findByUserId(actorUser.getId())
            .map(employee -> leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId()).stream()
                .map(this::toResponse)
                .toList())
            .orElse(List.of());
    }

    public List<LeaveRequestHistoryResponse> getLeaveRequestHistory(Long id) {
        LeaveRequest leaveRequest = getVisibleLeaveRequest(id);
        return leaveRequestHistoryRepository.findByLeaveRequestIdOrderByOccurredAtDesc(leaveRequest.getId()).stream()
            .map(this::toHistoryResponse)
            .toList();
    }

    @Transactional
    public LeaveRequestResponse approveLeaveRequest(Long id) {
        User actorUser = currentUserService.getCurrentUserEntity();
        LeaveRequest leaveRequest = findLeaveRequestForDecision(id, actorUser);
        assertPending(leaveRequest);

        leaveBalanceService.deductBalance(
            leaveRequest.getEmployee().getId(),
            leaveRequest.getType(),
            leaveRequest.getStartDate().getYear(),
            leaveRequest.getWorkingDays()
        );

        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setApprovedByUser(actorUser);
        leaveRequest.setRejectedByUser(null);
        leaveRequest.setCancelledByUser(null);
        leaveRequest.setRejectionReason(null);
        leaveRequest.setCancellationReason(null);
        leaveRequest.setCancelledAt(null);
        leaveRequest.setActionedAt(LocalDateTime.now());

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        recordHistory(saved, actorUser, LeaveActionType.APPROVED, LeaveStatus.PENDING, LeaveStatus.APPROVED, null);
        notifyEmployee(saved, "Leave approved", "Your leave request has been approved.");
        auditLogService.log(
            actorUser,
            "LEAVE_REQUEST_APPROVED",
            "LeaveRequest",
            saved.getId().toString(),
            "Leave request approved for " + saved.getEmployee().getFirstName() + " " + saved.getEmployee().getLastName(),
            null
        );
        return toResponse(saved);
    }

    @Transactional
    public LeaveRequestResponse rejectLeaveRequest(Long id, String reason) {
        User actorUser = currentUserService.getCurrentUserEntity();
        LeaveRequest leaveRequest = findLeaveRequestForDecision(id, actorUser);
        assertPending(leaveRequest);

        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setApprovedByUser(null);
        leaveRequest.setRejectedByUser(actorUser);
        leaveRequest.setCancelledByUser(null);
        leaveRequest.setRejectionReason(reason);
        leaveRequest.setCancellationReason(null);
        leaveRequest.setCancelledAt(null);
        leaveRequest.setActionedAt(LocalDateTime.now());

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        recordHistory(saved, actorUser, LeaveActionType.REJECTED, LeaveStatus.PENDING, LeaveStatus.REJECTED, reason);
        notifyEmployee(saved, "Leave rejected", "Your leave request has been rejected.");
        auditLogService.log(
            actorUser,
            "LEAVE_REQUEST_REJECTED",
            "LeaveRequest",
            saved.getId().toString(),
            "Leave request rejected for " + saved.getEmployee().getFirstName() + " " + saved.getEmployee().getLastName(),
            reason
        );
        return toResponse(saved);
    }

    @Transactional
    public LeaveRequestResponse cancelLeaveRequest(Long id, String reason) {
        User actorUser = currentUserService.getCurrentUserEntity();
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", id));

        assertCanCancel(actorUser, leaveRequest);
        if (leaveRequest.getStatus() != LeaveStatus.PENDING && leaveRequest.getStatus() != LeaveStatus.APPROVED) {
            throw new BusinessRuleException("Only PENDING or APPROVED leave requests can be cancelled");
        }

        LeaveStatus previousStatus = leaveRequest.getStatus();
        if (previousStatus == LeaveStatus.APPROVED) {
            leaveBalanceService.restoreBalance(
                leaveRequest.getEmployee().getId(),
                leaveRequest.getType(),
                leaveRequest.getStartDate().getYear(),
                leaveRequest.getWorkingDays()
            );
        }

        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        leaveRequest.setCancelledByUser(actorUser);
        leaveRequest.setCancellationReason(reason);
        leaveRequest.setCancelledAt(LocalDateTime.now());
        leaveRequest.setActionedAt(LocalDateTime.now());

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        recordHistory(saved, actorUser, LeaveActionType.CANCELLED, previousStatus, LeaveStatus.CANCELLED, reason);
        notifyEmployee(saved, "Leave cancelled", "A leave request has been cancelled.");
        auditLogService.log(
            actorUser,
            "LEAVE_REQUEST_CANCELLED",
            "LeaveRequest",
            saved.getId().toString(),
            "Leave request cancelled for " + saved.getEmployee().getFirstName() + " " + saved.getEmployee().getLastName(),
            reason
        );
        return toResponse(saved);
    }

    private LeaveRequest getVisibleLeaveRequest(Long id) {
        User actorUser = currentUserService.getCurrentUserEntity();
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", id));
        assertCanViewLeave(actorUser, leaveRequest.getEmployee());
        return leaveRequest;
    }

    private LeaveRequest findLeaveRequestForDecision(Long id, User actorUser) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", id));
        assertCanApproveOrReject(actorUser, leaveRequest.getEmployee());
        return leaveRequest;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessRuleException("Start date cannot be after end date");
        }
    }

    private void assertPending(LeaveRequest leaveRequest) {
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessRuleException("Only PENDING leave requests can be processed");
        }
    }

    private void assertNoOverlap(Long employeeId, LocalDate startDate, LocalDate endDate) {
        boolean hasConflict = leaveRequestRepository.existsByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            employeeId,
            List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED),
            endDate,
            startDate
        );

        if (hasConflict) {
            throw new BusinessRuleException("This leave request overlaps with an existing pending or approved request");
        }
    }

    private void assertCanCreateLeave(User actorUser, Employee employee) {
        if (actorUser.getRole() == Role.ADMIN || actorUser.getRole() == Role.HR_MANAGER) {
            return;
        }

        Employee actorEmployee = employeeRepository.findByUserId(actorUser.getId())
            .orElseThrow(() -> new AccessDeniedException("Current account is not linked to an employee record"));

        if (!actorEmployee.getId().equals(employee.getId())) {
            throw new AccessDeniedException("You can only submit leave requests for yourself");
        }
    }

    private void assertCanViewLeave(User actorUser, Employee employee) {
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

        throw new AccessDeniedException("You do not have access to this leave request");
    }

    private void assertCanApproveOrReject(User actorUser, Employee employee) {
        if (actorUser.getRole() == Role.ADMIN || actorUser.getRole() == Role.HR_MANAGER) {
            return;
        }

        if (actorUser.getRole() == Role.DEPARTMENT_MANAGER && isInManagementScope(employee, actorUser)) {
            return;
        }

        throw new AccessDeniedException("You do not have permission to approve or reject this leave request");
    }

    private void assertCanCancel(User actorUser, LeaveRequest leaveRequest) {
        if (actorUser.getRole() == Role.ADMIN || actorUser.getRole() == Role.HR_MANAGER) {
            return;
        }

        Employee actorEmployee = employeeRepository.findByUserId(actorUser.getId())
            .orElseThrow(() -> new AccessDeniedException("Current account is not linked to an employee record"));

        if (!actorEmployee.getId().equals(leaveRequest.getEmployee().getId())) {
            throw new AccessDeniedException("You can only cancel your own leave requests");
        }
    }

    private boolean isInManagementScope(Employee employee, User actorUser) {
        return departmentScopeService.isEmployeeInManagedScope(employee, actorUser.getId());
    }

    private Specification<LeaveRequest> buildVisibilitySpecification(
            User actorUser,
            LeaveStatus status,
            LeaveType type,
            Long employeeId,
            Long departmentId) {
        Specification<LeaveRequest> specification = (root, query, cb) -> cb.conjunction();

        if (status != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (type != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }
        if (employeeId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("employee").get("id"), employeeId));
        }
        if (departmentId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("employee").get("department").get("id"), departmentId));
        }

        if (actorUser.getRole() == Role.ADMIN || actorUser.getRole() == Role.HR_MANAGER) {
            return specification;
        }

        if (actorUser.getRole() == Role.EMPLOYEE) {
            Long actorEmployeeId = employeeRepository.findByUserId(actorUser.getId())
                .map(Employee::getId)
                .orElse(-1L);
            return specification.and((root, query, cb) -> cb.equal(root.get("employee").get("id"), actorEmployeeId));
        }

        if (actorUser.getRole() == Role.DEPARTMENT_MANAGER) {
            Set<Long> managedDepartmentIds = departmentScopeService.getManagedDepartmentIds(actorUser.getId());
            if (managedDepartmentIds.isEmpty()) {
                return specification.and((root, query, cb) -> cb.disjunction());
            }
            return specification.and((root, query, cb) -> root.get("employee").get("department").get("id").in(managedDepartmentIds));
        }

        return specification;
    }

    private void recordHistory(
            LeaveRequest leaveRequest,
            User actorUser,
            LeaveActionType actionType,
            LeaveStatus fromStatus,
            LeaveStatus toStatus,
            String note) {
        leaveRequestHistoryRepository.save(LeaveRequestHistory.builder()
            .leaveRequest(leaveRequest)
            .actorUser(actorUser)
            .actionType(actionType)
            .fromStatus(fromStatus)
            .toStatus(toStatus)
            .note(note)
            .occurredAt(LocalDateTime.now())
            .build());
    }

    private void notifyApprovers(LeaveRequest leaveRequest, User actorUser) {
        Set<User> recipients = new LinkedHashSet<>(userRepository.findByRoleIn(List.of(Role.ADMIN, Role.HR_MANAGER)));

        Department department = leaveRequest.getEmployee().getDepartment();
        while (department != null) {
            if (department.getManager() != null) {
                recipients.add(department.getManager());
            }
            department = department.getParentDepartment();
        }

        recipients.removeIf(user -> user.getId().equals(actorUser.getId()));

        for (User recipient : recipients) {
            notificationService.create(
                recipient,
                NotificationType.LEAVE_EVENT,
                "Leave request submitted",
                leaveRequest.getEmployee().getFirstName() + " " + leaveRequest.getEmployee().getLastName()
                    + " submitted a " + leaveRequest.getType() + " leave request.",
                "/leaves"
            );
        }
    }

    private void notifyEmployee(LeaveRequest leaveRequest, String title, String message) {
        User employeeUser = leaveRequest.getEmployee().getUser();
        notificationService.create(
            employeeUser,
            NotificationType.LEAVE_EVENT,
            title,
            message,
            "/leaves"
        );
    }

    private LeaveRequestHistoryResponse toHistoryResponse(LeaveRequestHistory history) {
        return new LeaveRequestHistoryResponse(
            history.getId(),
            history.getActionType(),
            history.getFromStatus(),
            history.getToStatus(),
            history.getActorUser() != null ? history.getActorUser().getId() : null,
            history.getActorUser() != null ? history.getActorUser().getEmail() : null,
            history.getNote(),
            history.getOccurredAt()
        );
    }

    private LeaveRequestResponse toResponse(LeaveRequest leaveRequest) {
        return new LeaveRequestResponse(
            leaveRequest.getId(),
            leaveRequest.getEmployee().getId(),
            leaveRequest.getEmployee().getFirstName() + " " + leaveRequest.getEmployee().getLastName(),
            leaveRequest.getType(),
            leaveRequest.getStartDate(),
            leaveRequest.getEndDate(),
            leaveRequest.getWorkingDays(),
            leaveRequest.getStatus(),
            leaveRequest.getReason(),
            leaveRequest.getApprovedByUser() != null ? leaveRequest.getApprovedByUser().getId() : null,
            leaveRequest.getApprovedByUser() != null ? leaveRequest.getApprovedByUser().getEmail() : null,
            leaveRequest.getRejectedByUser() != null ? leaveRequest.getRejectedByUser().getId() : null,
            leaveRequest.getRejectedByUser() != null ? leaveRequest.getRejectedByUser().getEmail() : null,
            leaveRequest.getCancelledByUser() != null ? leaveRequest.getCancelledByUser().getId() : null,
            leaveRequest.getCancelledByUser() != null ? leaveRequest.getCancelledByUser().getEmail() : null,
            leaveRequest.getActionedAt(),
            leaveRequest.getCancelledAt(),
            leaveRequest.getRejectionReason(),
            leaveRequest.getCancellationReason(),
            leaveRequest.getCreatedAt()
        );
    }
}
