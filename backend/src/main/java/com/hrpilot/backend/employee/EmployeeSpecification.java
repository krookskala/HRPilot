package com.hrpilot.backend.employee;

import org.springframework.data.jpa.domain.Specification;

public final class EmployeeSpecification {

    private EmployeeSpecification() {}

    public static Specification<Employee> hasNameContaining(String search) {
        return (root, query, cb) -> {
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("firstName")), pattern),
                cb.like(cb.lower(root.get("lastName")), pattern)
            );
        };
    }

    public static Specification<Employee> hasDepartmentId(Long departmentId) {
        return (root, query, cb) ->
            cb.equal(root.get("department").get("id"), departmentId);
    }

    public static Specification<Employee> hasPosition(String position) {
        return (root, query, cb) ->
            cb.like(cb.lower(root.get("position")), "%" + position.toLowerCase() + "%");
    }

    public static Specification<Employee> hasDepartmentIdIn(java.util.Collection<Long> departmentIds) {
        return (root, query, cb) ->
            root.get("department").get("id").in(departmentIds);
    }

    public static Specification<Employee> hasUserId(Long userId) {
        return (root, query, cb) ->
            cb.equal(root.get("user").get("id"), userId);
    }
}
