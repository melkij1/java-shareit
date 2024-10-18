package ru.practicum.shareit.item.repository;

import org.springframework.stereotype.Component;
import ru.practicum.shareit.item.model.Item;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ItemRepositoryImpl implements ItemRepository{
    private static int generatorId = 0;
    private final Map<Long, Item> items = new HashMap<>();
    private final Map<Long, List<Item>> userItemIndex = new LinkedHashMap<>();

    @Override
    public Optional<Item> findById(long itemId) {
        return Optional.ofNullable(items.get(itemId));
    }

    @Override
    public List<Item> findItemsByOwner(long userId) {
        return userItemIndex.get(userId);
    }

    @Override
    public List<Item> findItemBySearch(String text) {
        return items.values().stream()
                .filter(item -> item.getAvailable() && ((item.getName().toLowerCase().contains(text.toLowerCase())) ||
                        (item.getDescription().toLowerCase().contains(text.toLowerCase()))))
                .collect(Collectors.toList());
    }

    @Override
    public Item save(Item item, long userId) {
        item.setId(++generatorId);
        item.setOwnerId(userId);
        items.put(item.getId(), item);
        final List<Item> itemsByOwner = userItemIndex.computeIfAbsent(item.getOwnerId(), k -> new ArrayList<>());
        itemsByOwner.add(item);
        return item;
    }
}
