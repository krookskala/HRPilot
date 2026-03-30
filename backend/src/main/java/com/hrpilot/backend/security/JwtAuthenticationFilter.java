package com.hrpilot.backend.security;

import com.hrpilot.backend.user.User;
import com.hrpilot.backend.user.UserRepository;
import com.hrpilot.backend.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
    HttpServletResponse response, FilterChain filterChain) throws
    ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ") || authHeader.length() <= 7) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (jwtService.isTokenValid(token) &&
        SecurityContextHolder.getContext().getAuthentication() == null) {
            String email = jwtService.extractEmail(token);

            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty() || !userOpt.get().isActive()) {
                filterChain.doFilter(request, response);
                return;
            }

            String role = userOpt.get().getRole().name();

            AuthenticatedUser principal = new AuthenticatedUser(
                userOpt.get().getId(),
                email,
                userOpt.get().getRole()
            );

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );

            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
