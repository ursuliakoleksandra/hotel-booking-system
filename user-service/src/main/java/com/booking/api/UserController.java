package com.booking.api;

import com.booking.api.dto.UserDTO;
import com.booking.application.UserService;
import com.booking.domain.User;
import jakarta.validation.Valid; // Важливо додати цей імпорт
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users") // Додали v1 для версійності
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserApiMapper userApiMapper;

    @PostMapping
    public UserDTO create(@Valid @RequestBody UserDTO dto) { // @Valid запускає перевірку
        User user = userApiMapper.toDomain(dto);
        User saved = userService.createUser(user);
        return userApiMapper.toDto(saved);
    }

    @GetMapping
    public List<UserDTO> getAll() {
        return userService.getAllUsers().stream()
                .map(userApiMapper::toDto)
                .toList();
    }
}