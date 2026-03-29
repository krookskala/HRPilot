package com.hrpilot.backend.auth;

import com.hrpilot.backend.auth.dto.AuthRequest;
import com.hrpilot.backend.auth.dto.AuthResponse;
import com.hrpilot.backend.auth.dto.RegisterRequest;
import com.hrpilot.backend.auth.dto.TokenRefreshRequest;
import com.hrpilot.backend.auth.dto.TokenRefreshResponse;
import com.hrpilot.backend.security.JwtService;
import com.hrpilot.backend.user.User;
import com.hrpilot.backend.user.Role;
import com.hrpilot.backend.user.UserRepository;
import com.hrpilot.backend.user.dto.CreateUserRequest;
import com.hrpilot.backend.user.dto.UserResponse;
import com.hrpilot.backend.user.UserService;
import com.hrpilot.backend.common.exception.AuthenticationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request for email: {}", request.email());
        CreateUserRequest userRequest = new CreateUserRequest(
            request.email(),
            request.password(),
            Role.EMPLOYEE
        );
        UserResponse response = userService.createUser(userRequest);
        log.info("User registered successfully with id: {}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("Login attempt for email: {}", request.email());
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> {
                log.warn("Login failed - email not found: {}", request.email());
                return new AuthenticationException("Invalid email or password");
            });

        if (!user.isActive()) {
            log.warn("Login failed - account deactivated: {}", request.email());
            throw new AuthenticationException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Login failed - wrong password for email: {}", request.email());
            throw new AuthenticationException("Invalid email or password");
        }

        String accessToken = jwtService.generateToken(user.getEmail(), user.getRole().name());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("Login successful for email: {}", request.email());
        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken.getToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(request.refreshToken());
        User user = refreshToken.getUser();

        String newAccessToken = jwtService.generateToken(user.getEmail(), user.getRole().name());
        log.info("Token refreshed for email: {}", user.getEmail());

        return ResponseEntity.ok(new TokenRefreshResponse(newAccessToken, refreshToken.getToken()));
    }
}
