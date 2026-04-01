package com.hrpilot.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrpilot.backend.audit.AuditLogService;
import com.hrpilot.backend.auth.dto.AcceptInvitationRequest;
import com.hrpilot.backend.auth.dto.AuthRequest;
import com.hrpilot.backend.auth.dto.PasswordResetRequest;
import com.hrpilot.backend.notification.NotificationService;
import com.hrpilot.backend.security.JwtAuthenticationFilter;
import com.hrpilot.backend.security.JwtService;
import com.hrpilot.backend.user.Role;
import com.hrpilot.backend.user.User;
import com.hrpilot.backend.user.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(com.hrpilot.backend.config.SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @MockitoBean
    private InvitationTokenService invitationTokenService;

    @MockitoBean
    private PasswordResetTokenService passwordResetTokenService;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private com.hrpilot.backend.employee.EmployeeRepository employeeRepository;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any(FilterChain.class));
    }

    @Test
    void login_success_returnsToken() throws Exception {
        AuthRequest request = new AuthRequest("test@test.com", "password123");
        User user = User.builder()
            .id(1L)
            .email("test@test.com")
            .passwordHash("hashed")
            .role(Role.EMPLOYEE)
            .active(true)
            .build();
        RefreshToken refreshToken = RefreshToken.builder()
            .id(1L)
            .token("refresh-token-123")
            .user(user)
            .expiryDate(Instant.now().plusSeconds(3600))
            .build();

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken("test@test.com", "EMPLOYEE")).thenReturn("jwt-token-123");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("jwt-token-123"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token-123"));
    }

    @Test
    void getInvitation_success_returnsDetails() throws Exception {
        User user = User.builder()
            .id(1L)
            .email("invitee@test.com")
            .role(Role.EMPLOYEE)
            .preferredLang("en")
            .build();
        InvitationToken invitationToken = InvitationToken.builder()
            .id(1L)
            .token("invite-token")
            .user(user)
            .expiresAt(Instant.now().plusSeconds(3600))
            .createdAt(LocalDateTime.now())
            .build();

        when(invitationTokenService.validateInvitation("invite-token")).thenReturn(invitationToken);
        when(invitationTokenService.toDetails(invitationToken))
            .thenReturn(new com.hrpilot.backend.auth.dto.InvitationDetailsResponse(
                "invitee@test.com", Role.EMPLOYEE, "en", invitationToken.getExpiresAt()
            ));

        mockMvc.perform(get("/api/auth/invitations/invite-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("invitee@test.com"))
            .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    void acceptInvitation_success_returnsTokens() throws Exception {
        AcceptInvitationRequest request = new AcceptInvitationRequest("invite-token", "Password123!");
        User user = User.builder()
            .id(1L)
            .email("invitee@test.com")
            .role(Role.EMPLOYEE)
            .active(false)
            .build();
        InvitationToken invitationToken = InvitationToken.builder()
            .token("invite-token")
            .user(user)
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
        RefreshToken refreshToken = RefreshToken.builder()
            .token("refresh-token-123")
            .user(user)
            .expiryDate(Instant.now().plusSeconds(3600))
            .build();

        when(invitationTokenService.validateInvitation("invite-token")).thenReturn(invitationToken);
        when(jwtService.generateToken("invitee@test.com", "EMPLOYEE")).thenReturn("jwt-token-123");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        mockMvc.perform(post("/api/auth/invitations/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("jwt-token-123"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token-123"));
    }

    @Test
    void requestPasswordReset_success_returnsResetLink() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest("test@test.com");
        User user = User.builder()
            .id(1L)
            .email("test@test.com")
            .build();
        PasswordResetToken resetToken = PasswordResetToken.builder()
            .id(1L)
            .token("reset-token")
            .user(user)
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenService.createResetToken(user)).thenReturn(resetToken);

        mockMvc.perform(post("/api/auth/password/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void login_invalidPassword_returns401() throws Exception {
        AuthRequest request = new AuthRequest("test@test.com", "wrong");
        User user = User.builder()
            .id(1L)
            .email("test@test.com")
            .passwordHash("hashed")
            .role(Role.EMPLOYEE)
            .active(true)
            .build();

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("AUTHENTICATION_FAILED"));
    }
}
