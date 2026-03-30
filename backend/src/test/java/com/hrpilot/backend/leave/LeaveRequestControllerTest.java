package com.hrpilot.backend.leave;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrpilot.backend.common.exception.BusinessRuleException;
import com.hrpilot.backend.leave.dto.CreateLeaveRequest;
import com.hrpilot.backend.leave.dto.LeaveActionRequest;
import com.hrpilot.backend.leave.dto.LeaveRequestResponse;
import com.hrpilot.backend.security.JwtAuthenticationFilter;
import com.hrpilot.backend.security.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import com.hrpilot.backend.config.DataWebConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrpilot.backend.config.SecurityConfig;

@WebMvcTest(LeaveRequestController.class)
@Import({SecurityConfig.class, DataWebConfig.class})
class LeaveRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LeaveRequestService leaveRequestService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private LeaveBalanceService leaveBalanceService;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any(FilterChain.class));
    }

    private LeaveRequestResponse buildResponse(LeaveStatus status) {
        return new LeaveRequestResponse(
            1L,
            1L,
            "John Doe",
            LeaveType.ANNUAL,
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 4, 5),
            3,
            status,
            "Vacation",
            null,
            null,
            status == LeaveStatus.REJECTED ? 5L : null,
            status == LeaveStatus.REJECTED ? "manager@hrpilot.com" : null,
            null,
            null,
            null,
            null,
            status == LeaveStatus.REJECTED ? "Not enough coverage" : null,
            null,
            java.time.LocalDateTime.of(2026, 3, 1, 10, 0)
        );
    }

    @Test
    @WithMockUser
    void createLeaveRequest_returns201() throws Exception {
        CreateLeaveRequest request = new CreateLeaveRequest(
            1L, LeaveType.ANNUAL,
            LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 5), "Vacation");
        when(leaveRequestService.createLeaveRequest(any())).thenReturn(buildResponse(LeaveStatus.PENDING));

        mockMvc.perform(post("/api/leave-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.employeeFullName").value("John Doe"))
            .andExpect(jsonPath("$.workingDays").value(3));
    }

    @Test
    @WithMockUser
    void getAllLeaveRequests_returns200() throws Exception {
        Page<LeaveRequestResponse> page = new PageImpl<>(List.of(buildResponse(LeaveStatus.PENDING)));
        when(leaveRequestService.getVisibleLeaveRequests(any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/leave-requests"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].type").value("ANNUAL"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void approveLeaveRequest_asAdmin_returns200() throws Exception {
        when(leaveRequestService.approveLeaveRequest(1L)).thenReturn(buildResponse(LeaveStatus.APPROVED));

        mockMvc.perform(put("/api/leave-requests/1/approve"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void approveLeaveRequest_asEmployee_returns403() throws Exception {
        mockMvc.perform(put("/api/leave-requests/1/approve"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR_MANAGER")
    void rejectLeaveRequest_asHRManager_returns200() throws Exception {
        when(leaveRequestService.rejectLeaveRequest(eq(1L), eq("Not enough coverage")))
            .thenReturn(buildResponse(LeaveStatus.REJECTED));

        mockMvc.perform(put("/api/leave-requests/1/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LeaveActionRequest("Not enough coverage"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void rejectLeaveRequest_asEmployee_returns403() throws Exception {
        mockMvc.perform(put("/api/leave-requests/1/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LeaveActionRequest("No"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DEPARTMENT_MANAGER")
    void approveLeaveRequest_asDeptManager_returns200() throws Exception {
        when(leaveRequestService.approveLeaveRequest(1L)).thenReturn(buildResponse(LeaveStatus.APPROVED));

        mockMvc.perform(put("/api/leave-requests/1/approve"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void approveLeaveRequest_alreadyApproved_returns422() throws Exception {
        when(leaveRequestService.approveLeaveRequest(1L))
            .thenThrow(new BusinessRuleException("Only PENDING leave requests can be processed"));

        mockMvc.perform(put("/api/leave-requests/1/approve"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    @WithMockUser
    void createLeaveRequest_invalidRequest_returns400() throws Exception {
        CreateLeaveRequest request = new CreateLeaveRequest(null, null, null, null, null);

        mockMvc.perform(post("/api/leave-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getLeaveRequestsByEmployee_returns200() throws Exception {
        when(leaveRequestService.getLeaveRequestsByEmployee(1L))
            .thenReturn(List.of(buildResponse(LeaveStatus.PENDING)));

        mockMvc.perform(get("/api/leave-requests/employee/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].employeeId").value(1));
    }
}
