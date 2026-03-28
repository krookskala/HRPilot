package com.hrpilot.backend.payroll;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrpilot.backend.payroll.dto.CreatePayrollRequest;
import com.hrpilot.backend.payroll.dto.PayrollResponse;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.common.exception.BusinessRuleException;
import com.hrpilot.backend.config.JwtService;
import com.hrpilot.backend.config.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import com.hrpilot.backend.config.SecurityConfig;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PayrollController.class)
@Import(SecurityConfig.class)
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
        return new PayrollResponse(1L, 1L, "John Doe", 2026, 3,
                new BigDecimal("5000"), new BigDecimal("500"),
                new BigDecimal("300"), new BigDecimal("5200"), status);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createPayroll_asAdmin_returns201() throws Exception {
        CreatePayrollRequest request = new CreatePayrollRequest(
                1L, 2026, 3, new BigDecimal("5000"),
                new BigDecimal("500"), new BigDecimal("300"));
        when(payrollService.createPayroll(any())).thenReturn(buildResponse(PayrollStatus.DRAFT));

        mockMvc.perform(post("/api/payrolls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.netSalary").value(5200))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void createPayroll_asEmployee_returns403() throws Exception {
        CreatePayrollRequest request = new CreatePayrollRequest(
                1L, 2026, 3, new BigDecimal("5000"),
                new BigDecimal("500"), new BigDecimal("300"));

        mockMvc.perform(post("/api/payrolls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR_MANAGER")
    void createPayroll_asHRManager_returns201() throws Exception {
        CreatePayrollRequest request = new CreatePayrollRequest(
                1L, 2026, 3, new BigDecimal("5000"),
                new BigDecimal("500"), new BigDecimal("300"));
        when(payrollService.createPayroll(any())).thenReturn(buildResponse(PayrollStatus.DRAFT));

        mockMvc.perform(post("/api/payrolls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void getAllPayrolls_returns200() throws Exception {
        Page<PayrollResponse> page = new PageImpl<>(List.of(buildResponse(PayrollStatus.DRAFT)));
        when(payrollService.getAllPayrolls(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/payrolls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].year").value(2026));
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
        when(payrollService.getPayrollsByEmployee(1L))
                .thenReturn(List.of(buildResponse(PayrollStatus.DRAFT)));

        mockMvc.perform(get("/api/payrolls/employee/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeFullName").value("John Doe"));
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
