package ru.practicum.service.users;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.users.NewUserRequest;
import ru.practicum.dto.users.UserDto;
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
        User newUser = usersRepository.save(userMapper.newUserRequestToUser(newUserRequest));

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
    @Transactional
    public void deleteUser(Long userId) {
        usersRepository.findByIdOrThrow(userId);
        usersRepository.deleteById(userId);
    }
}
