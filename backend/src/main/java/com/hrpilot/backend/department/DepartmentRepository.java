package com.hrpilot.backend.department;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByName(String name);
    List<Department> findByParentDepartmentId(Long parentDepartmentId);
}
