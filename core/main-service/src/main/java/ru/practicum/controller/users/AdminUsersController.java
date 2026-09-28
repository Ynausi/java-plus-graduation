package ru.practicum.controller.users;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.users.NewUserRequest;
import ru.practicum.dto.users.UserDto;
import ru.practicum.service.users.UserService;

import java.util.List;

@RestController
@RequestMapping(path = "/admin/users")
@RequiredArgsConstructor
public class AdminUsersController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody @Valid NewUserRequest newUserRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(newUserRequest));
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getUser(
            @RequestParam(required = false) List<Long> ids,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {
        return ResponseEntity.ok(userService.getUsers(ids, from, size));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
