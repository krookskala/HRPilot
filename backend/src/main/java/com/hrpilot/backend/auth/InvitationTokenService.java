package com.hrpilot.backend.auth;

import com.hrpilot.backend.auth.dto.InvitationDetailsResponse;
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
public class InvitationTokenService {

    private final InvitationTokenRepository invitationTokenRepository;

    @Value("${app.invitation.expiration-ms:259200000}")
    private long invitationExpirationMs;

    @Transactional
    public InvitationToken createInvitation(User user, User createdBy) {
        invitationTokenRepository.deleteByUserId(user.getId());
        invitationTokenRepository.deleteExpired(Instant.now());

        InvitationToken token = InvitationToken.builder()
            .token(UUID.randomUUID().toString())
            .user(user)
            .createdBy(createdBy)
            .expiresAt(Instant.now().plusMillis(invitationExpirationMs))
            .build();

        return invitationTokenRepository.save(token);
    }

    public InvitationToken validateInvitation(String token) {
        InvitationToken invitationToken = invitationTokenRepository.findByToken(token)
            .orElseThrow(() -> new AuthenticationException("Invalid invitation token"));

        if (invitationToken.getConsumedAt() != null) {
            throw new AuthenticationException("Invitation token has already been used");
        }

        if (invitationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthenticationException("Invitation token expired");
        }

        return invitationToken;
    }

    @Transactional
    public void consume(InvitationToken invitationToken) {
        invitationToken.setConsumedAt(LocalDateTime.now());
        invitationTokenRepository.save(invitationToken);
    }

    public InvitationDetailsResponse toDetails(InvitationToken invitationToken) {
        User user = invitationToken.getUser();
        return new InvitationDetailsResponse(
            user.getEmail(),
            user.getRole(),
            user.getPreferredLang(),
            invitationToken.getExpiresAt()
        );
    }
}
