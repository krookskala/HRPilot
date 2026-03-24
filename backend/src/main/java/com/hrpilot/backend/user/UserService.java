package com.hrpilot.backend.user;

import com.hrpilot.backend.user.dto.CreateUserRequest;
import com.hrpilot.backend.user.dto.UserResponse;
import com.hrpilot.backend.user.dto.UpdateUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email Already Exists");
        }

        User user = User.builder()
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .role(request.role())
            .build();

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
            .map(user -> toResponse(user))
            .toList();
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User Not Found"));
        return toResponse(user);
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User Not Found"));
        
        user.setRole(request.role());
        user.setActive(request.isActive());
        user.setPreferredLang(request.preferredLang());

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User Not Found");
        }
        userRepository.deleteById(id);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getRole(),
            user.isActive(),
            user.getPreferredLang()
        );
    }
}