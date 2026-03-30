package com.hrpilot.backend.auth;

import com.hrpilot.backend.common.exception.AuthenticationException;
import com.hrpilot.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${app.password-reset.expiration-ms:3600000}")
    private long resetExpirationMs;

    @Transactional
    public PasswordResetToken createResetToken(User user) {
        passwordResetTokenRepository.deleteByUserId(user.getId());
        passwordResetTokenRepository.deleteExpired(Instant.now());

        PasswordResetToken token = PasswordResetToken.builder()
            .token(UUID.randomUUID().toString())
            .user(user)
            .expiresAt(Instant.now().plusMillis(resetExpirationMs))
            .build();

        return passwordResetTokenRepository.save(token);
    }

    public PasswordResetToken validate(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
            .orElseThrow(() -> new AuthenticationException("Invalid password reset token"));

        if (resetToken.getConsumedAt() != null) {
            throw new AuthenticationException("Password reset token has already been used");
        }

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthenticationException("Password reset token expired");
        }

        return resetToken;
    }

    @Transactional
    public void consume(PasswordResetToken resetToken) {
        resetToken.setConsumedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);
    }
}
