package com.hrpilot.backend.user;

import com.hrpilot.backend.user.dto.CreateUserRequest;
import com.hrpilot.backend.user.dto.UpdateUserRequest;
import com.hrpilot.backend.user.dto.UserResponse;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.common.exception.DuplicateResourceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_success() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest("test@test.com", "password123", Role.EMPLOYEE);
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        User savedUser = User.builder()
                .id(1L)
                .email("test@test.com")
                .passwordHash("hashedPassword")
                .role(Role.EMPLOYEE)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserResponse response = userService.createUser(request);

        // Assert
        assertNotNull(response);
        assertEquals("test@test.com", response.email());
        assertEquals(Role.EMPLOYEE, response.role());
    }

    @Test
    void createUser_emailAlreadyExists_throwsException() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest("exists@test.com", "password123", Role.EMPLOYEE);
        when(userRepository.existsByEmail("exists@test.com")).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> userService.createUser(request));
    }

    @Test
    void getUserById_userExists_returnsResponse() {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .passwordHash("hashed")
                .role(Role.EMPLOYEE)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        UserResponse response = userService.getUserById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("test@test.com", response.email());
    }

    @Test
    void getUserById_userNotFound_throwsException() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    @Test
    void getAllUsers_returnsAllUsers() {
        // Arrange
        User user1 = User.builder().id(1L).email("a@test.com").role(Role.EMPLOYEE).build();
        User user2 = User.builder().id(2L).email("b@test.com").role(Role.ADMIN).build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user1, user2), pageable, 2);
        when(userRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<UserResponse> responses = userService.getAllUsers(pageable);

        // Assert
        assertEquals(2, responses.getTotalElements());
        assertEquals("a@test.com", responses.getContent().get(0).email());
        assertEquals("b@test.com", responses.getContent().get(1).email());
    }

    @Test
    void updateUser_success() {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .passwordHash("hashed")
                .role(Role.EMPLOYEE)
                .build();
        UpdateUserRequest request = new UpdateUserRequest(Role.ADMIN, true, "tr");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        UserResponse response = userService.updateUser(1L, request);

        // Assert
        assertNotNull(response);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateUser_userNotFound_throwsException() {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest(Role.ADMIN, true, "en");
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> userService.updateUser(99L, request));
    }

    @Test
    void deleteUser_userExists_deletesSuccessfully() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(true);

        // Act
        userService.deleteUser(1L);

        // Assert
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteUser_userNotFound_throwsException() {
        // Arrange
        when(userRepository.existsById(99L)).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(99L));
    }
}
