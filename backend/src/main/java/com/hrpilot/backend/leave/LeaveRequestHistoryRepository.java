package com.hrpilot.backend.leave;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestHistoryRepository extends JpaRepository<LeaveRequestHistory, Long> {
    List<LeaveRequestHistory> findByLeaveRequestIdOrderByOccurredAtDesc(Long leaveRequestId);
    void deleteByLeaveRequestEmployeeId(Long employeeId);
}
