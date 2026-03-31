package com.hrpilot.backend.payroll;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PayrollRepository extends JpaRepository<PayrollRecord, Long> {
    @Override
    @EntityGraph(attributePaths = {"employee", "run"})
    Page<PayrollRecord> findAll(Pageable pageable);

    List<PayrollRecord> findByEmployeeId(Long employeeId);

    @EntityGraph(attributePaths = {"employee", "run"})
    List<PayrollRecord> findByEmployeeIdOrderByYearDescMonthDesc(Long employeeId);

    @EntityGraph(attributePaths = {"employee", "run"})
    List<PayrollRecord> findByEmployeeIdInOrderByCreatedAtDesc(Collection<Long> employeeIds);
    Page<PayrollRecord> findByEmployeeIdOrderByYearDescMonthDesc(Long employeeId, Pageable pageable);

    @EntityGraph(attributePaths = {"employee", "run"})
    List<PayrollRecord> findByRunIdOrderByEmployeeIdAsc(Long runId);

    @Override
    @EntityGraph(attributePaths = {"employee", "run"})
    Optional<PayrollRecord> findById(Long id);

    boolean existsByEmployeeIdAndYearAndMonth(Long employeeId, int year, int month);
    Optional<PayrollRecord> findByIdAndEmployeeId(Long id, Long employeeId);
    long countByEmployeeIdAndStatusIn(Long employeeId, Collection<PayrollStatus> statuses);
    long countByRunId(Long runId);

    @Query("SELECT p.status, COUNT(p), COALESCE(SUM(p.netSalary), 0) FROM PayrollRecord p GROUP BY p.status")
    List<Object[]> countAndSumGroupByStatus();

    @Query("SELECT p.status, COUNT(p), COALESCE(SUM(p.netSalary), 0) FROM PayrollRecord p WHERE p.employee.id IN :employeeIds GROUP BY p.status")
    List<Object[]> countAndSumGroupByStatusForEmployees(@Param("employeeIds") Collection<Long> employeeIds);
    void deleteByEmployeeId(Long employeeId);
}
