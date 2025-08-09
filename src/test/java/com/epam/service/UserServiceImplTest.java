package com.epam.service;

import com.epam.dto.UserCreateDTO;
import com.epam.dto.UserDTO;
import com.epam.dto.UserUpdateDTO;
import com.epam.exceptions.ResourceAlreadyExistsException;
import com.epam.exceptions.ResourceNotFoundException;
import com.epam.mapper.UserMapper;
import com.epam.model.User;
import com.epam.repository.UserRepository;
import com.epam.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {
    @Mock
    private UserRepository repository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserDTO testUserDTO;
    private UserCreateDTO testUserCreateDTO;
    private UserUpdateDTO testUserUpdateDTO;
    private List<User> userList;
    private List<UserDTO> userDTOList;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        testUserDTO = new UserDTO();
        testUserDTO.setId(1L);
        testUserDTO.setUsername("testuser");
        testUserDTO.setEmail("test@example.com");
        testUserDTO.setFirstName("Test");
        testUserDTO.setLastName("User");
        testUserDTO.setActive(true);

        testUserCreateDTO = new UserCreateDTO();
        testUserCreateDTO.setUsername("newuser");
        testUserCreateDTO.setEmail("new@example.com");
        testUserCreateDTO.setPassword("password123");
        testUserCreateDTO.setFirstName("New");
        testUserCreateDTO.setLastName("User");

        testUserUpdateDTO = new UserUpdateDTO();
        testUserUpdateDTO.setUsername("updateduser");
        testUserUpdateDTO.setEmail("updated@example.com");
        testUserUpdateDTO.setFirstName("Updated");
        testUserUpdateDTO.setLastName("User");

        userList = new ArrayList<>();
        userList.add(testUser);

        userDTOList = new ArrayList<>();
        userDTOList.add(testUserDTO);
    }

    @Test
    void getAll_ShouldReturnPaginatedUsers() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
        Page<User> userPage = new PageImpl<>(userList, pageable, 1);

        when(repository.findAll(any(Pageable.class))).thenReturn(userPage);
        when(userMapper.toDtoList(anyList())).thenReturn(userDTOList);

        // Act
        ResponseEntity<Map<String, Object>> response = userService.getAll(0, 10, "id", "asc");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> responseBody = response.getBody();
        assertEquals(userDTOList, responseBody.get("content"));
        assertEquals(0, responseBody.get("currentPage"));
        assertEquals(1L, responseBody.get("totalItems"));
        assertEquals(1, responseBody.get("totalPages"));

        verify(repository).findAll(any(Pageable.class));
        verify(userMapper).toDtoList(userList);
    }

    @Test
    void getById_WhenUserExists_ShouldReturnUser() {
        // Arrange
        when(repository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toDto(testUser)).thenReturn(testUserDTO);

        // Act
        ResponseEntity<UserDTO> response = userService.getById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testUserDTO, response.getBody());

        verify(repository).findById(1L);
        verify(userMapper).toDto(testUser);
    }

    @Test
    void getById_WhenUserDoesNotExist_ShouldThrowException() {
        // Arrange
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> userService.getById(999L));
        verify(repository).findById(999L);
    }

    @Test
    void create_WhenValidData_ShouldCreateUser() {
        // Arrange
        User userFromDto = new User();
        userFromDto.setUsername("newuser");
        userFromDto.setEmail("new@example.com");
        userFromDto.setPassword("password123"); // This should match testUserCreateDTO's password
        userFromDto.setFirstName("New");
        userFromDto.setLastName("User");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("newuser");
        savedUser.setEmail("new@example.com");
        savedUser.setPassword("encodedPassword");
        savedUser.setFirstName("New");
        savedUser.setLastName("User");
        savedUser.setActive(true);

        when(repository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(repository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userMapper.toEntity(testUserCreateDTO)).thenReturn(userFromDto);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(repository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(testUserDTO);

        // Act
        ResponseEntity<UserDTO> response = userService.create(testUserCreateDTO);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(testUserDTO, response.getBody());

        verify(repository).findByUsername(testUserCreateDTO.getUsername());
        verify(repository).findByEmail(testUserCreateDTO.getEmail());
        verify(userMapper).toEntity(testUserCreateDTO);
        verify(passwordEncoder).encode("password123");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertEquals("encodedPassword", capturedUser.getPassword());

        verify(userMapper).toDto(savedUser);
    }

    @Test
    void create_WhenUsernameExists_ShouldThrowException() {
        // Arrange
        when(repository.findByUsername(testUserCreateDTO.getUsername())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(ResourceAlreadyExistsException.class, () -> userService.create(testUserCreateDTO));

        verify(repository).findByUsername(testUserCreateDTO.getUsername());
        verifyNoMoreInteractions(repository, userMapper, passwordEncoder);
    }

    @Test
    void create_WhenEmailExists_ShouldThrowException() {
        // Arrange
        when(repository.findByUsername(testUserCreateDTO.getUsername())).thenReturn(Optional.empty());
        when(repository.findByEmail(testUserCreateDTO.getEmail())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(ResourceAlreadyExistsException.class, () -> userService.create(testUserCreateDTO));

        verify(repository).findByUsername(testUserCreateDTO.getUsername());
        verify(repository).findByEmail(testUserCreateDTO.getEmail());
        verifyNoMoreInteractions(repository, userMapper, passwordEncoder);
    }

    @Test
    void update_WhenUserExistsAndValidData_ShouldUpdateUser() {
        // Arrange
        when(repository.findById(1L)).thenReturn(Optional.of(testUser));

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("updateduser");

        when(userMapper.toEntity(eq(testUserUpdateDTO), any(User.class))).thenReturn(updatedUser);
        when(repository.save(updatedUser)).thenReturn(updatedUser);
        when(userMapper.toDto(updatedUser)).thenReturn(testUserDTO);

        // Act
        ResponseEntity<UserDTO> response = userService.update(1L, testUserUpdateDTO);

        // Assert
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(testUserDTO, response.getBody());

        verify(repository).findById(1L);
        verify(userMapper).toEntity(eq(testUserUpdateDTO), any(User.class));
        verify(repository).save(updatedUser);
        verify(userMapper).toDto(updatedUser);
    }

    @Test
    void delete_WhenUserExists_ShouldDeactivateUser() {
        // Arrange
        when(repository.findById(1L)).thenReturn(Optional.of(testUser));
        when(repository.save(any(User.class))).thenReturn(testUser);

        // Act
        ResponseEntity<Map<String, String>> response = userService.delete(1L);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        verify(repository).findById(1L);

        // Capture the user being saved to verify it's deactivated
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertFalse(capturedUser.isActive());
        assertNotNull(capturedUser.getUpdatedAt());
    }

    @Test
    void delete_WhenUserDoesNotExist_ShouldThrowException() {
        // Arrange
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> userService.delete(999L));

        verify(repository).findById(999L);
        verifyNoMoreInteractions(repository);
    }

}
