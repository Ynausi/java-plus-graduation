package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.NewUserRequest;
import ru.practicum.dto.UserDto;
import ru.practicum.exceptions.UserAlreadyExistsException;
import ru.practicum.exceptions.UserNotFoundException;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.User;
import ru.practicum.repository.UsersRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UsersRepository usersRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest newUserRequest) {
        if (usersRepository.existsByEmail(newUserRequest.getEmail())) {
            throw new UserAlreadyExistsException(
                    "User with email=" + newUserRequest.getEmail() + " already exists"
            );
        }

        User newUser = usersRepository.save(
                userMapper.newUserRequestToUser(newUserRequest)
        );

        return userMapper.userToUserDto(newUser);
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, Integer from, Integer size) {
        List<User> users = ids != null && !ids.isEmpty() ?
                usersRepository.findByIdIn(ids) :
                usersRepository.findAllWithOffsetLimit(from, size);

        return users.stream()
                .map(userMapper::userToUserDto)
                .toList();
    }

    @Override
    public UserDto getUser(Long userId) {
       UserDto response = userMapper.userToUserDto(
               usersRepository.findById(userId).orElseThrow(() ->
                       new UserNotFoundException("No user with id:" + userId)));
       return response;
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        usersRepository.findByIdOrThrow(userId);
        usersRepository.deleteById(userId);
    }
}
