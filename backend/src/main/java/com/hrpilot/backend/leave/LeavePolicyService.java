package com.hrpilot.backend.leave;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LeavePolicyService {

    private final LeavePolicyRepository leavePolicyRepository;

    public Map<LeaveType, LeavePolicy> getPoliciesByType() {
        Map<LeaveType, LeavePolicy> policies = new EnumMap<>(LeaveType.class);
        List<LeavePolicy> savedPolicies = leavePolicyRepository.findAll();
        for (LeavePolicy policy : savedPolicies) {
            policies.put(policy.getLeaveType(), policy);
        }
        return policies;
    }

    public int getAnnualDays(LeaveType leaveType) {
        return leavePolicyRepository.findByLeaveType(leaveType)
            .map(LeavePolicy::getAnnualDays)
            .orElseGet(() -> switch (leaveType) {
                case ANNUAL -> 30;
                case SICK -> 15;
                case UNPAID -> 10;
            });
    }
}
