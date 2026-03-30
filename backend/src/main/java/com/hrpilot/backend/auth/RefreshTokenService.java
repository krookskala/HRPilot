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
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration; // 7 days default

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshExpiration))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedAtIsNull(token)
                .orElseThrow(() -> new AuthenticationException("Invalid refresh token"));

        if (!refreshToken.getUser().isActive()) {
            refreshToken.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(refreshToken);
            throw new AuthenticationException("User account is inactive");
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshToken.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(refreshToken);
            throw new AuthenticationException("Refresh token expired. Please login again.");
        }

        return refreshToken;
    }

    @Transactional
    public RefreshToken rotateRefreshToken(String token) {
        RefreshToken existingToken = verifyRefreshToken(token);
        existingToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(existingToken);
        return createRefreshToken(existingToken.getUser());
    }

    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByTokenAndRevokedAtIsNull(token)
            .ifPresent(refreshToken -> {
                refreshToken.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(refreshToken);
            });
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId)
            .forEach(token -> {
                token.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(token);
            });
    }
}
