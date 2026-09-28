package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.users.NewUserRequest;
import ru.practicum.dto.users.UserDto;
import ru.practicum.dto.users.UserShortDto;
import ru.practicum.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto userToUserDto(User user);

    UserShortDto userToUserShortDto(User user);

    @Mapping(target = "id", ignore = true)
    User newUserRequestToUser(NewUserRequest newUserRequest);
}