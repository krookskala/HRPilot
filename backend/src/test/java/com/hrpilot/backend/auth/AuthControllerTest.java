package com.hrpilot.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrpilot.backend.auth.dto.AuthRequest;
import com.hrpilot.backend.auth.dto.RegisterRequest;
import com.hrpilot.backend.security.JwtService;
import com.hrpilot.backend.security.JwtAuthenticationFilter;
import com.hrpilot.backend.user.Role;
import com.hrpilot.backend.user.User;
import com.hrpilot.backend.user.UserRepository;
import com.hrpilot.backend.user.UserService;
import com.hrpilot.backend.user.dto.UserResponse;
import com.hrpilot.backend.common.exception.DuplicateResourceException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import com.hrpilot.backend.config.SecurityConfig;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any(FilterChain.class));
    }

    @Test
    void register_success_returns201() throws Exception {
        RegisterRequest request = new RegisterRequest("new@test.com", "password123");
        UserResponse response = new UserResponse(1L, "new@test.com", Role.EMPLOYEE, true, "en");
        when(userService.createUser(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@test.com"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest request = new RegisterRequest("exists@test.com", "password123");
        when(userService.createUser(any()))
                .thenThrow(new DuplicateResourceException("User", "email", "exists@test.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void login_success_returnsToken() throws Exception {
        AuthRequest request = new AuthRequest("test@test.com", "password123");
        User user = User.builder()
                .id(1L).email("test@test.com")
                .passwordHash("hashed").role(Role.EMPLOYEE)
                .isActive(true).build();
        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L).token("refresh-token-123").user(user).build();
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken("test@test.com", "EMPLOYEE")).thenReturn("jwt-token-123");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-123"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        AuthRequest request = new AuthRequest("test@test.com", "wrong");
        User user = User.builder()
                .id(1L).email("test@test.com")
                .passwordHash("hashed").role(Role.EMPLOYEE)
                .isActive(true).build();
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void login_emailNotFound_returns401() throws Exception {
        AuthRequest request = new AuthRequest("noone@test.com", "password123");
        when(userRepository.findByEmail("noone@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_deactivatedAccount_returns401() throws Exception {
        AuthRequest request = new AuthRequest("test@test.com", "password123");
        User user = User.builder()
                .id(1L).email("test@test.com")
                .passwordHash("hashed").role(Role.EMPLOYEE)
                .isActive(false).build();
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Account is deactivated"));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("not-an-email", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("test@test.com", "12345");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
