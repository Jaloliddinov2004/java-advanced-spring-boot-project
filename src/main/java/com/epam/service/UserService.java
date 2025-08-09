package com.epam.service;

import com.epam.dto.UserCreateDTO;
import com.epam.dto.UserDTO;
import com.epam.dto.UserUpdateDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface UserService {

    ResponseEntity<Map<String, Object>> getAll(int page, int size, String sortBy, String direction);

    ResponseEntity<UserDTO> getById(Long id);

    ResponseEntity<UserDTO> create(@Valid UserCreateDTO dto);

    ResponseEntity<UserDTO> update(Long id, @Valid UserUpdateDTO dto);

    ResponseEntity<Map<String, String>> delete(Long id);
}
