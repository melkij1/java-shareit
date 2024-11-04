package ru.practicum.shareit.user.service;

import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

public interface UserService {
    List<UserDto> findAll();

    UserDto getUserById(long id);

    UserDto saveNewUser(UserDto userDto);

    UserDto updateUser(long id, UserDto userDto);

    void deleteUserById(long id);
}
