package ru.practicum.service.users;

import ru.practicum.dto.users.NewUserRequest;
import ru.practicum.dto.users.UserDto;

import java.util.List;

public interface UserService {
    UserDto createUser(NewUserRequest newUserRequest);

    List<UserDto> getUsers(List<Long> ids, Integer from, Integer size);

    void deleteUser(Long userId);
}
