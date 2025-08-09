package com.epam.service.impl;

import com.epam.dto.UserCreateDTO;
import com.epam.dto.UserDTO;
import com.epam.dto.UserUpdateDTO;
import com.epam.exceptions.ResourceAlreadyExistsException;
import com.epam.exceptions.ResourceNotFoundException;
import com.epam.mapper.UserMapper;
import com.epam.model.User;
import com.epam.repository.UserRepository;
import com.epam.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getAll(int page, int size, String sortBy, String direction) {
        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException e) {
            sortDirection = Sort.Direction.ASC;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<User> userPage = repository.findAll(pageable);

        List<UserDTO> users = userMapper.toDtoList(userPage.getContent());

        Map<String, Object> response = buildPaginationResponse(users, userPage, sortBy, direction);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<UserDTO> getById(Long id) {
        User user = findById(id);

        UserDTO userDTO = userMapper.toDto(user);
        return new ResponseEntity<>(userDTO, HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<UserDTO> create(UserCreateDTO dto) {
        validateUsername(dto.getUsername());

        validateEmail(dto.getEmail());

        User user = userMapper.toEntity(dto);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User savedUser = repository.save(user);

        return new ResponseEntity<>(userMapper.toDto(savedUser), HttpStatus.CREATED);
    }

    @Override
    @Transactional
    public ResponseEntity<UserDTO> update(Long id, UserUpdateDTO dto) {
        User user = findById(id);

        user = userMapper.toEntity(dto, user);

        UserDTO response = userMapper.toDto(repository.save(user));

        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    @Override
    @Transactional
    public ResponseEntity<Map<String, String>> delete(Long id) {
        User user = findById(id);
        user.setActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        repository.save(user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private void validateUsername(String username) {
        if (repository.findByUsername(username).isPresent()) {
            throw new ResourceAlreadyExistsException("User with Username: " + username + " is already exists");
        }
    }

    private void validateEmail(String email) {
        if (repository.findByEmail(email).isPresent()) {
            throw new ResourceAlreadyExistsException("User with email: " + email + " is already exists");
        }
    }

    private User findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with this ID: " + id + " is not found"));
    }

    private <T> Map<String, Object> buildPaginationResponse(
            List<T> data,
            Page<?> page,
            String sortBy,
            String direction) {

        Map<String, Object> response = new HashMap<>();
        response.put("content", data);
        response.put("currentPage", page.getNumber());
        response.put("totalItems", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());
        response.put("size", page.getSize());
        response.put("first", page.isFirst());
        response.put("last", page.isLast());
        response.put("sort", sortBy);
        response.put("direction", direction);

        return response;
    }
}
