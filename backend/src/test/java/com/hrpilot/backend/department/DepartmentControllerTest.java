package com.hrpilot.backend.department;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrpilot.backend.department.dto.CreateDepartmentRequest;
import com.hrpilot.backend.department.dto.DepartmentResponse;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.common.exception.DuplicateResourceException;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DepartmentController.class)
@Import({SecurityConfig.class, DataWebConfig.class})
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DepartmentService departmentService;

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

    private DepartmentResponse buildResponse() {
        return new DepartmentResponse(1L, "Engineering", "manager@test.com", null, null);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createDepartment_asAdmin_returns201() throws Exception {
        CreateDepartmentRequest request = new CreateDepartmentRequest("Engineering", null, null);
        when(departmentService.createDepartment(any())).thenReturn(buildResponse());

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Engineering"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void createDepartment_asEmployee_returns403() throws Exception {
        CreateDepartmentRequest request = new CreateDepartmentRequest("Engineering", null, null);

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR_MANAGER")
    void createDepartment_asHRManager_returns403() throws Exception {
        CreateDepartmentRequest request = new CreateDepartmentRequest("Engineering", null, null);

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void getAllDepartments_returns200() throws Exception {
        Page<DepartmentResponse> page = new PageImpl<>(List.of(buildResponse()));
        when(departmentService.getAllDepartments(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Engineering"));
    }

    @Test
    @WithMockUser
    void getDepartmentById_exists_returns200() throws Exception {
        when(departmentService.getDepartmentById(1L)).thenReturn(buildResponse());

        mockMvc.perform(get("/api/departments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.managerEmail").value("manager@test.com"));
    }

    @Test
    @WithMockUser
    void getDepartmentById_notFound_returns404() throws Exception {
        when(departmentService.getDepartmentById(99L))
                .thenThrow(new ResourceNotFoundException("Department", "id", 99L));

        mockMvc.perform(get("/api/departments/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createDepartment_duplicateName_returns409() throws Exception {
        CreateDepartmentRequest request = new CreateDepartmentRequest("Engineering", null, null);
        when(departmentService.createDepartment(any()))
                .thenThrow(new DuplicateResourceException("Department", "name", "Engineering"));

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteDepartment_asAdmin_returns204() throws Exception {
        doNothing().when(departmentService).deleteDepartment(1L);

        mockMvc.perform(delete("/api/departments/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void deleteDepartment_asEmployee_returns403() throws Exception {
        mockMvc.perform(delete("/api/departments/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createDepartment_invalidRequest_returns400() throws Exception {
        CreateDepartmentRequest request = new CreateDepartmentRequest("", null, null);

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
