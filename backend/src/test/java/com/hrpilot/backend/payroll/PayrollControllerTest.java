package com.hrpilot.backend.payroll;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrpilot.backend.common.exception.BusinessRuleException;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.config.DataWebConfig;
import com.hrpilot.backend.config.SecurityConfig;
import com.hrpilot.backend.payroll.dto.CreatePayrollRequest;
import com.hrpilot.backend.payroll.dto.CreatePayrollRunRequest;
import com.hrpilot.backend.payroll.dto.PayrollResponse;
import com.hrpilot.backend.payroll.dto.PayrollRunResponse;
import com.hrpilot.backend.security.JwtAuthenticationFilter;
import com.hrpilot.backend.security.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PayrollController.class)
@Import({SecurityConfig.class, DataWebConfig.class})
class PayrollControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PayrollService payrollService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any(FilterChain.class));
    }

    private PayrollResponse buildResponse(PayrollStatus status) {
        return new PayrollResponse(
            1L,
            1L,
            "John Doe",
            2026,
            3,
            new BigDecimal("5000.00"),
            new BigDecimal("5500.00"),
            new BigDecimal("500.00"),
            new BigDecimal("1800.00"),
            new BigDecimal("700.00"),
            new BigDecimal("710.00"),
            new BigDecimal("1100.00"),
            new BigDecimal("3700.00"),
            "I",
            status,
            10L,
            status == PayrollStatus.DRAFT ? null : LocalDateTime.of(2026, 3, 28, 12, 0),
            status == PayrollStatus.PAID ? LocalDateTime.of(2026, 3, 29, 9, 0) : null,
            status != PayrollStatus.DRAFT,
            List.of()
        );
    }

    private CreatePayrollRequest buildCreateRequest() {
        return new CreatePayrollRequest(
            1L,
            2026,
            3,
            new BigDecimal("5000.00"),
            new BigDecimal("500.00"),
            new BigDecimal("300.00"),
            "I"
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createPayroll_asAdmin_returns201() throws Exception {
        when(payrollService.createPayroll(any())).thenReturn(buildResponse(PayrollStatus.DRAFT));

        mockMvc.perform(post("/api/payrolls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildCreateRequest())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.netSalary").value(3700.00))
            .andExpect(jsonPath("$.taxClass").value("I"))
            .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void createPayroll_asEmployee_returns403() throws Exception {
        mockMvc.perform(post("/api/payrolls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildCreateRequest())))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR_MANAGER")
    void createPayroll_asHRManager_returns201() throws Exception {
        when(payrollService.createPayroll(any())).thenReturn(buildResponse(PayrollStatus.DRAFT));

        mockMvc.perform(post("/api/payrolls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildCreateRequest())))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "HR_MANAGER")
    void getAllPayrolls_returns200() throws Exception {
        Page<PayrollResponse> page = new PageImpl<>(List.of(buildResponse(PayrollStatus.DRAFT)));
        when(payrollService.getAllPayrolls(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/payrolls"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].year").value(2026))
            .andExpect(jsonPath("$.content[0].taxClass").value("I"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createPayrollRun_asAdmin_returns201() throws Exception {
        CreatePayrollRunRequest request = new CreatePayrollRunRequest(
            "March Run",
            2026,
            3,
            List.of(1L),
            false,
            new BigDecimal("500.00"),
            new BigDecimal("300.00"),
            "I"
        );
        when(payrollService.createPayrollRun(any())).thenReturn(new PayrollRunResponse(
            5L,
            "March Run",
            2026,
            3,
            PayrollRunStatus.DRAFT,
            1,
            LocalDateTime.of(2026, 3, 28, 10, 0),
            null,
            null
        ));

        mockMvc.perform(post("/api/payrolls/runs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("March Run"))
            .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void markAsPaid_asAdmin_returns200() throws Exception {
        when(payrollService.markAsPaid(1L)).thenReturn(buildResponse(PayrollStatus.PAID));

        mockMvc.perform(put("/api/payrolls/1/pay"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void markAsPaid_asEmployee_returns403() throws Exception {
        mockMvc.perform(put("/api/payrolls/1/pay"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void markAsPaid_alreadyPaid_returns422() throws Exception {
        when(payrollService.markAsPaid(1L))
            .thenThrow(new BusinessRuleException("Payroll record is already marked as paid"));

        mockMvc.perform(put("/api/payrolls/1/pay"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    @WithMockUser
    void getPayrollsByEmployee_returns200() throws Exception {
        when(payrollService.getPayrollsByEmployee(org.mockito.ArgumentMatchers.eq(1L), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(buildResponse(PayrollStatus.PUBLISHED))));

        mockMvc.perform(get("/api/payrolls/employee/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].employeeFullName").value("John Doe"))
            .andExpect(jsonPath("$.content[0].status").value("PUBLISHED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void markAsPaid_notFound_returns404() throws Exception {
        when(payrollService.markAsPaid(99L))
            .thenThrow(new ResourceNotFoundException("PayrollRecord", "id", 99L));

        mockMvc.perform(put("/api/payrolls/99/pay"))
            .andExpect(status().isNotFound());
    }
}
