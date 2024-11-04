package ru.practicum.shareit.item.repository;

import ru.practicum.shareit.item.model.Item;

import java.util.List;
import java.util.Optional;

public interface ItemRepository {
    Optional<Item> findById(long itemId);

    List<Item> findItemsByOwner(long userId);

    List<Item> findItemBySearch(String text);

    Item save(Item item, long userId);
}
