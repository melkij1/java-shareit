package ru.practicum.shareit.user;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.practicum.shareit.exception.EntityNotFoundException;
import ru.practicum.shareit.exception.NotUniqueEmailException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.user.service.UserServiceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserServiceImpl userService;

    private final long id = 1L;
    private final UserDto userDto = new UserDto(id, "User", "user@mail.ru");
    private final User user = new User(id, "User", "user@mail.ru");


    @Test
    void findAll_whenNoUsers_thenReturnsEmptyList() {
        when(userRepository.findAll()).thenReturn(new ArrayList<>());

        List<UserDto> result = userService.findAll();

        assertTrue(result.isEmpty());
    }


    @Test
    void getAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserDto> targetUsers = userService.findAll();

        Assertions.assertNotNull(targetUsers);
        assertEquals(1, targetUsers.size());
        verify(userRepository, times(1))
                .findAll();
    }

    @Test
    void getUserById_whenUserFound_thenReturnedUser() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        UserDto actualUser = userService.getUserById(id);

        assertEquals(UserMapper.toUserDto(user), actualUser);
    }

    @Test
    void getUserById_whenUserNotFound_thenExceptionThrown() {
        when((userRepository).findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.getUserById(2L));
    }

    @Test
    void saveNewUser_whenUserNameValid_thenSavedUser() {
        when(userRepository.save(any())).thenReturn(user);

        UserDto actualUser = userService.saveNewUser(userDto);

        assertEquals(userDto, actualUser);
    }

    @Test
    void saveNewUser_whenEmailNotUnique_thenThrowsNotUniqueEmailException() {
        when(userRepository.save(any())).thenThrow(new NotUniqueEmailException("Email already exists"));

        assertThrows(NotUniqueEmailException.class, () -> {
            userService.saveNewUser(userDto);
        });
    }

    @Test
    void saveNewUser_whenUserEmailDuplicate_thenNotSavedUser() {
        doThrow(DataIntegrityViolationException.class).when(userRepository).save(any(User.class));

        assertThrows(DataIntegrityViolationException.class, () -> userService.saveNewUser(userDto));
    }

    @Test
    void updateUser_whenUserFound_thenUpdatedOnlyAvailableFields() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        UserDto actualUser = userService.updateUser(id, userDto);

        assertEquals(UserMapper.toUserDto(user), actualUser);
        verify(userRepository, times(1))
                .findById(user.getId());
    }

    @Test
    void deleteUser() {
        userService.deleteUserById(1L);
        verify(userRepository, times(1))
                .deleteById(1L);
    }

    @Test
    public void testValidateUniqueEmail_EmailExists() {
        when(userRepository.findAll()).thenReturn(List.of(new User(id, "Some User", "user@mail.ru")));

        NotUniqueEmailException thrown = assertThrows(NotUniqueEmailException.class, () -> {
            userService.saveNewUser(userDto);
        });

        assertEquals("Пользователь с email user@mail.ru уже существует", thrown.getMessage());
    }

    @Test
    public void testValidateUniqueEmail_EmailDoesNotExist() {
        UserDto uniqueUserDto = new UserDto(id, "Unique User", "unique@example.com");
        when(userRepository.findAll()).thenReturn(new ArrayList<>());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        assertDoesNotThrow(() -> userService.saveNewUser(uniqueUserDto));
    }

    @Test
    void updateUser_whenUserNotFound_thenThrowsException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.updateUser(id, userDto));
    }

    @Test
    void saveNewUser_whenUserDtoIsNull_thenThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> userService.saveNewUser (null));
    }


    @Test
    void updateUser_whenUserNameIsBlank_thenUserNameNotUpdated() {
        UserDto userDtoWithBlankName = new UserDto(id, "   ", "newemail@example.com");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        UserDto actualUser  = userService.updateUser (id, userDtoWithBlankName);

        assertEquals(user.getName(), actualUser .getName()); // Имя не должно измениться
        verify(userRepository, times(1)).findById(id);
    }

    @Test
    void updateUser_whenUserEmailIsBlank_thenUserEmailNotUpdated() {
        UserDto userDtoWithBlankEmail = new UserDto(id, "New Name", "   ");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        UserDto actualUser  = userService.updateUser (id, userDtoWithBlankEmail);

        assertEquals(user.getEmail(), actualUser .getEmail()); // Email не должен измениться
        verify(userRepository, times(1)).findById(id);
    }

    @Test
    void updateUser_whenUserEmailIsNotUnique_thenThrowsNotUniqueEmailException() {
        UserDto userDtoWithDuplicateEmail = new UserDto(id, "New Name", "duplicate@example.com");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.findAll()).thenReturn(List.of(new User(id, "Existing User", "duplicate@example.com")));

        assertThrows(NotUniqueEmailException.class, () -> userService.updateUser (id, userDtoWithDuplicateEmail));
    }

    @Test
    void deleteUserById_whenUserDoesNotExist_thenThrowsEntityNotFoundException() {
        doThrow(new EntityNotFoundException("Пользователь не найден")).when(userRepository).deleteById(anyLong());

        assertThrows(EntityNotFoundException.class, () -> userService.deleteUserById(2L));
    }

}
