package com.epam.mapper;

import com.epam.dto.UserCreateDTO;
import com.epam.dto.UserDTO;
import com.epam.dto.UserUpdateDTO;
import com.epam.model.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class UserMapper {

    public List<UserDTO> toDtoList(List<User> users) {
        if (users.isEmpty()) {
            return new ArrayList<>();
        }

        return users.stream()
                .map(this::toDto)
                .toList();
    }

    public UserDTO toDto(User user) {
        if (user == null) {
            return null;
        }

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setActive(user.isActive());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        return dto;
    }

    public User toEntity(UserCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setCreatedAt(LocalDateTime.now());

        return user;
    }

    public User toEntity(UserUpdateDTO dto, User user) {
        if (dto == null) {
            return null;
        }

        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setUpdatedAt(LocalDateTime.now());

        return user;
    }
}
