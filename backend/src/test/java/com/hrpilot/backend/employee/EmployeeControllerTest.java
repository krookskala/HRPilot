package com.hrpilot.backend.employee;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrpilot.backend.employee.dto.CreateEmployeeRequest;
import com.hrpilot.backend.employee.dto.EmployeeResponse;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.security.JwtService;
import com.hrpilot.backend.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import com.hrpilot.backend.config.DataWebConfig;
import com.hrpilot.backend.config.SecurityConfig;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
@Import({SecurityConfig.class, DataWebConfig.class})
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeService employeeService;

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

    private EmployeeResponse buildResponse() {
        return new EmployeeResponse(1L, "emp@test.com", "John", "Doe",
                "Developer", new BigDecimal("5000"), LocalDate.of(2024, 1, 15), null,
                1L, "Engineering");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createEmployee_asAdmin_returns201() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                1L, "John", "Doe", "Developer",
                new BigDecimal("5000"), LocalDate.of(2024, 1, 15), 1L);
        when(employeeService.createEmployee(any())).thenReturn(buildResponse());

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void createEmployee_asEmployee_returns403() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                1L, "John", "Doe", "Developer",
                new BigDecimal("5000"), LocalDate.of(2024, 1, 15), 1L);

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR_MANAGER")
    void createEmployee_asHRManager_returns201() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                1L, "John", "Doe", "Developer",
                new BigDecimal("5000"), LocalDate.of(2024, 1, 15), 1L);
        when(employeeService.createEmployee(any())).thenReturn(buildResponse());

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void getAllEmployees_returns200() throws Exception {
        Page<EmployeeResponse> page = new PageImpl<>(List.of(buildResponse()));
        when(employeeService.getAllEmployees(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].firstName").value("John"));
    }

    @Test
    @WithMockUser
    void getEmployeeById_exists_returns200() throws Exception {
        when(employeeService.getEmployeeById(1L)).thenReturn(buildResponse());

        mockMvc.perform(get("/api/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("emp@test.com"));
    }

    @Test
    @WithMockUser
    void getEmployeeById_notFound_returns404() throws Exception {
        when(employeeService.getEmployeeById(99L))
                .thenThrow(new ResourceNotFoundException("Employee", "id", 99L));

        mockMvc.perform(get("/api/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteEmployee_asAdmin_returns204() throws Exception {
        doNothing().when(employeeService).deleteEmployee(1L);

        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void deleteEmployee_asEmployee_returns403() throws Exception {
        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createEmployee_invalidRequest_returns400() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                null, "", "", "",
                null, null, null);

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllEmployees_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isForbidden());
    }
}
