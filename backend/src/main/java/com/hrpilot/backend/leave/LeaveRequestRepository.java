package com.hrpilot.backend.leave;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long>, JpaSpecificationExecutor<LeaveRequest> {
    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
    List<LeaveRequest> findByEmployeeIdInOrderByCreatedAtDesc(Collection<Long> employeeIds);
    boolean existsByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        Long employeeId,
        Collection<LeaveStatus> statuses,
        LocalDate endDate,
        LocalDate startDate
    );
    long countByStatus(LeaveStatus status);
    long countByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);
    long countByEmployeeIdInAndStatus(Collection<Long> employeeIds, LeaveStatus status);

    @Query("SELECT l.status, COUNT(l) FROM LeaveRequest l GROUP BY l.status")
    List<Object[]> countGroupByStatus();

    @Query("SELECT l.status, COUNT(l) FROM LeaveRequest l WHERE l.employee.id IN :employeeIds GROUP BY l.status")
    List<Object[]> countGroupByStatusForEmployees(@Param("employeeIds") Collection<Long> employeeIds);

    @EntityGraph(attributePaths = {"employee"})
    @Query("SELECT l FROM LeaveRequest l ORDER BY l.createdAt DESC")
    Page<LeaveRequest> findRecentWithEmployee(Pageable pageable);

    @EntityGraph(attributePaths = {"employee"})
    @Query("SELECT l FROM LeaveRequest l WHERE l.employee.id IN :employeeIds ORDER BY l.createdAt DESC")
    List<LeaveRequest> findByEmployeeIdInWithEmployee(@Param("employeeIds") Collection<Long> employeeIds);
    void deleteByEmployeeId(Long employeeId);
}
