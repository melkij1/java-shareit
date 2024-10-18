package ru.practicum.shareit.user.repository;

import org.springframework.stereotype.Component;
import ru.practicum.shareit.exception.EntityNotFoundException;
import ru.practicum.shareit.user.model.User;

import java.util.*;

@Component
public class UserRepositoryImpl implements UserRepository{
    private static int generatedId = 0;
    private final Map<Long, User> users = new HashMap<>();

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public User save(User user) {
        user.setId(++generatedId);
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> findById(long userId) {
        return Optional.ofNullable(users.get(userId));
    }

    @Override
    public void deleteById(long userId) {
        if (!users.containsKey(userId)) {
           throw new EntityNotFoundException(String.format("Объект класса %s не найден", User.class));
        }
        users.remove(userId);
    }
}
