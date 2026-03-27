package com.hrpilot.backend.department;

import com.hrpilot.backend.department.dto.CreateDepartmentRequest;
import com.hrpilot.backend.department.dto.DepartmentResponse;
import com.hrpilot.backend.user.User;
import com.hrpilot.backend.user.UserRepository;
import com.hrpilot.backend.user.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DepartmentService departmentService;

    private Department buildDepartment() {
        return Department.builder()
                .id(1L)
                .name("Engineering")
                .build();
    }

    @Test
    void createDepartment_success() {
        // Arrange
        CreateDepartmentRequest request = new CreateDepartmentRequest("Engineering", null, null);
        when(departmentRepository.existsByName("Engineering")).thenReturn(false);
        Department saved = buildDepartment();
        when(departmentRepository.save(any(Department.class))).thenReturn(saved);

        // Act
        DepartmentResponse response = departmentService.createDepartment(request);

        // Assert
        assertNotNull(response);
        assertEquals("Engineering", response.name());
    }

    @Test
    void createDepartment_nameAlreadyExists_throwsException() {
        // Arrange
        CreateDepartmentRequest request = new CreateDepartmentRequest("Engineering", null, null);
        when(departmentRepository.existsByName("Engineering")).thenReturn(true);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> departmentService.createDepartment(request));
    }

    @Test
    void createDepartment_withManager_success() {
        // Arrange
        User manager = User.builder().id(1L).email("manager@test.com").role(Role.HR_MANAGER).build();
        CreateDepartmentRequest request = new CreateDepartmentRequest("HR", 1L, null);
        when(departmentRepository.existsByName("HR")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
        Department saved = Department.builder().id(2L).name("HR").manager(manager).build();
        when(departmentRepository.save(any(Department.class))).thenReturn(saved);

        // Act
        DepartmentResponse response = departmentService.createDepartment(request);

        // Assert
        assertNotNull(response);
        assertEquals("manager@test.com", response.managerEmail());
    }

    @Test
    void createDepartment_managerNotFound_throwsException() {
        // Arrange
        CreateDepartmentRequest request = new CreateDepartmentRequest("HR", 99L, null);
        when(departmentRepository.existsByName("HR")).thenReturn(false);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> departmentService.createDepartment(request));
    }

    @Test
    void createDepartment_withParent_success() {
        // Arrange
        Department parent = Department.builder().id(1L).name("Engineering").build();
        CreateDepartmentRequest request = new CreateDepartmentRequest("Backend", null, 1L);
        when(departmentRepository.existsByName("Backend")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(parent));
        Department saved = Department.builder().id(2L).name("Backend").parentDepartment(parent).build();
        when(departmentRepository.save(any(Department.class))).thenReturn(saved);

        // Act
        DepartmentResponse response = departmentService.createDepartment(request);

        // Assert
        assertNotNull(response);
        assertEquals("Engineering", response.parentDepartmentName());
    }

    @Test
    void createDepartment_parentNotFound_throwsException() {
        // Arrange
        CreateDepartmentRequest request = new CreateDepartmentRequest("Backend", null, 99L);
        when(departmentRepository.existsByName("Backend")).thenReturn(false);
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> departmentService.createDepartment(request));
    }

    @Test
    void getAllDepartments_returnsAllDepartments() {
        // Arrange
        Department dept = buildDepartment();
        when(departmentRepository.findAll()).thenReturn(List.of(dept));

        // Act
        List<DepartmentResponse> responses = departmentService.getAllDepartments();

        // Assert
        assertEquals(1, responses.size());
        assertEquals("Engineering", responses.get(0).name());
    }

    @Test
    void getDepartmentById_departmentExists_returnsResponse() {
        // Arrange
        Department dept = buildDepartment();
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));

        // Act
        DepartmentResponse response = departmentService.getDepartmentById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Engineering", response.name());
    }

    @Test
    void getDepartmentById_departmentNotFound_throwsException() {
        // Arrange
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> departmentService.getDepartmentById(99L));
    }

    @Test
    void deleteDepartment_departmentExists_deletesSuccessfully() {
        // Arrange
        when(departmentRepository.existsById(1L)).thenReturn(true);

        // Act
        departmentService.deleteDepartment(1L);

        // Assert
        verify(departmentRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteDepartment_departmentNotFound_throwsException() {
        // Arrange
        when(departmentRepository.existsById(99L)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> departmentService.deleteDepartment(99L));
    }
}
