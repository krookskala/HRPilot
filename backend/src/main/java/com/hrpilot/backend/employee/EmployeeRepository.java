package com.hrpilot.backend.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
    Optional<Employee> findByUserId(Long userId);

    @Query("SELECT e FROM Employee e LEFT JOIN FETCH e.department WHERE e.user.id = :userId")
    Optional<Employee> findByUserIdWithDepartment(@Param("userId") Long userId);
    boolean existsByUserId(Long userId);
    boolean existsByDepartmentId(Long departmentId);
    List<Employee> findByDepartmentIdIn(Collection<Long> departmentIds);
    long countByDepartmentIdIn(Collection<Long> departmentIds);

    @Query("SELECT e.department.name, COUNT(e) FROM Employee e WHERE e.department IS NOT NULL GROUP BY e.department.name")
    List<Object[]> countGroupByDepartmentName();
}
