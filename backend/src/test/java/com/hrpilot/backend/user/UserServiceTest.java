package com.hrpilot.backend.user;

import com.hrpilot.backend.user.dto.CreateUserRequest;
import com.hrpilot.backend.user.dto.UpdateUserRequest;
import com.hrpilot.backend.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

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
        assertThrows(RuntimeException.class, () -> userService.createUser(request));
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
        assertThrows(RuntimeException.class, () -> userService.getUserById(99L));
    }

    @Test
    void getAllUsers_returnsAllUsers() {
        // Arrange
        User user1 = User.builder().id(1L).email("a@test.com").role(Role.EMPLOYEE).build();
        User user2 = User.builder().id(2L).email("b@test.com").role(Role.ADMIN).build();
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        // Act
        List<UserResponse> responses = userService.getAllUsers();

        // Assert
        assertEquals(2, responses.size());
        assertEquals("a@test.com", responses.get(0).email());
        assertEquals("b@test.com", responses.get(1).email());
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
        assertThrows(RuntimeException.class, () -> userService.updateUser(99L, request));
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
        assertThrows(RuntimeException.class, () -> userService.deleteUser(99L));
    }
}
