package ru.practicum.shareit.item;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.practicum.shareit.booking.dto.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.enums.BookingStatusEnum;
import ru.practicum.shareit.exception.EntityNotFoundException;
import ru.practicum.shareit.exception.NotBookerException;
import ru.practicum.shareit.exception.NotOwnerException;
import ru.practicum.shareit.item.comment.Comment;
import ru.practicum.shareit.item.comment.CommentRepository;
import ru.practicum.shareit.item.comment.dto.CommentDtoIn;
import ru.practicum.shareit.item.comment.dto.CommentDtoOut;
import ru.practicum.shareit.item.comment.mapper.CommentMapper;
import ru.practicum.shareit.item.dto.ItemDtoIn;
import ru.practicum.shareit.item.dto.ItemDtoOut;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.item.service.ItemServiceImpl;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.ItemRequestRepository;
import ru.practicum.shareit.user.dto.UserDtoShort;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ItemRequestRepository requestRepository;
    @InjectMocks
    private ItemServiceImpl itemService;

    private final long id = 1L;
    private final long notOwnerId = 2L;
    private final User user = new User(id, "User", "user@mail.ru");
    private final User notOwner = new User(2L, "User2", "user2@mail.ru");
    private final ItemDtoIn itemDtoIn = new ItemDtoIn("item", "cool item", true, null);
    private final ItemDtoOut itemDtoOut = new ItemDtoOut(id, "item", "cool item", true,
            new UserDtoShort(id, "User"));
    private final Item item = new Item(id, "item", "cool item", true, user, null);
    private final Item anotherItem = new Item(id, "item2", "cool item", true, user, null);
    private final CommentDtoOut commentDto = new CommentDtoOut(id, "abc", "User",
            LocalDateTime.of(2023, 7, 1, 12, 12, 12));
    private final Comment comment = new Comment(id, "abc", item, user,
            LocalDateTime.of(2023, 7, 1, 12, 12, 12));
    private final Booking booking = new Booking(id, null, null, item, user, BookingStatusEnum.WAITING);
    private final ItemRequest itemRequest = new ItemRequest();

    @Test
    void shouldSaveItem_whenUserExists() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(itemRepository.save(any())).thenReturn(item);

        ItemDtoOut actualItemDto = itemService.saveNewItem(itemDtoIn, id);

        Assertions.assertEquals(ItemMapper.toDto(item), actualItemDto);
        Assertions.assertNull(item.getRequest());
    }

    @Test
    void shouldNotSaveItem_whenUserDoesNotExist() {
        when((userRepository).findById(2L)).thenReturn(Optional.empty());

        Assertions.assertThrows(EntityNotFoundException.class, () -> itemService.saveNewItem(itemDtoIn, 2L));
    }

    @Test
    void shouldNotSaveItem_whenItemNameIsMissing() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        doThrow(DataIntegrityViolationException.class).when(itemRepository).save(any(Item.class));

        Assertions.assertThrows(DataIntegrityViolationException.class, () -> itemService.saveNewItem(itemDtoIn, id));
    }

    @Test
    void shouldUpdateItem_whenUserIsTheOwner() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));

        ItemDtoOut actualItemDto = itemService.updateItem(id, itemDtoIn, id);

        Assertions.assertEquals(itemDtoOut, actualItemDto);
    }

    @Test
    void shouldNotUpdateItem_whenUserIsNotTheOwner() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(notOwner));
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));

        Assertions.assertThrows(NotOwnerException.class, () -> itemService.updateItem(id, itemDtoIn, 2L));
    }

    @Test
    void shouldReturnItem_whenItemExists() {
        when(bookingRepository.findFirstByItemIdAndStartLessThanEqualAndStatus(anyLong(), any(), any(), any()))
                .thenReturn(Optional.of(booking));
        when(bookingRepository.findFirstByItemIdAndStartAfterAndStatus(anyLong(), any(), any(), any()))
                .thenReturn(Optional.of(booking));
        when(commentRepository.findAllByItemId(id)).thenReturn(List.of(comment));
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));
        final ItemDtoOut itemDto = ItemMapper.toDto(item);
        itemDto.setLastBooking(BookingMapper.toBookingDtoShort(booking));
        itemDto.setNextBooking(BookingMapper.toBookingDtoShort(booking));
        itemDto.setComments(List.of(CommentMapper.toCommentDtoOut(comment)));

        ItemDtoOut actualItemDto = itemService.getItemById(id, id);

        Assertions.assertEquals(itemDto, actualItemDto);
    }

    @Test
    void shouldThrowException_whenItemDoesNotExist() {
        when((itemRepository).findById(2L)).thenReturn(Optional.empty());

        Assertions.assertThrows(EntityNotFoundException.class, () -> itemService.getItemById(2L, id));
    }

    @Test
    void shouldReturnItems_whenOwnerRequestsWithPaging() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(itemRepository.findAllByOwnerId(anyLong(), any())).thenReturn(List.of(item));

        List<ItemDtoOut> targetItems = itemService.getItemsByOwner(0, 10, id);

        Assertions.assertNotNull(targetItems);
        Assertions.assertEquals(1, targetItems.size());
        verify(itemRepository, times(1))
                .findAllByOwnerId(anyLong(), any());
    }

    @Test
    void shouldReturnItems_whenSearchTextIsProvided() {
        when(itemRepository.search(any(), any())).thenReturn(List.of(item));

        List<ItemDtoOut> targetItems = itemService.getItemBySearch(0, 10, "abc");

        Assertions.assertNotNull(targetItems);
        Assertions.assertEquals(1, targetItems.size());
        verify(itemRepository, times(1))
                .search(any(), any());
    }

    @Test
    void shouldReturnEmptyList_whenSearchTextIsBlank() {
        List<ItemDtoOut> targetItems = itemService.getItemBySearch(0, 10, "");

        Assertions.assertTrue(targetItems.isEmpty());
        Assertions.assertEquals(0, targetItems.size());
        verify(itemRepository, never()).search(any(), any());
    }

    @Test
    void shouldSaveComment_whenUserIsBooker() {
        when(bookingRepository.existsByBookerIdAndItemIdAndEndBefore(anyLong(), anyLong(), any()))
                .thenReturn(true);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(commentRepository.save(any())).thenReturn(comment);
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));

        CommentDtoOut actualComment = itemService.saveNewComment(id, new CommentDtoIn("abc"), id);

        Assertions.assertEquals(commentDto, actualComment);
    }

    @Test
    void shouldThrowException_whenUserIsNotBooker() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));
        when(bookingRepository.existsByBookerIdAndItemIdAndEndBefore(anyLong(), anyLong(), any())).thenReturn(false);

        Assertions.assertThrows(NotBookerException.class, () ->
                itemService.saveNewComment(id, new CommentDtoIn("abc"), id));
    }

    @Test
    void shouldThrowException_whenRequestDoesNotExist() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(requestRepository.findById(itemRequest.getId())).thenReturn(Optional.empty());

        itemDtoIn.setRequestId(itemRequest.getId());

        Assertions.assertThrows(EntityNotFoundException.class, () -> itemService.saveNewItem(itemDtoIn, id));
    }


    @Test
    void shouldUpdateItemAvailability_whenUserIsTheOwner() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));

        itemDtoIn.setAvailable(false);
        ItemDtoOut actualItemDto = itemService.updateItem(id, itemDtoIn, id);

        Assertions.assertFalse(actualItemDto.getAvailable());
    }

    @Test
    void shouldNotUpdateItem_whenNameIsBlank() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));

        itemDtoIn.setName(" ");

        ItemDtoOut actualItemDto = itemService.updateItem(id, itemDtoIn, id);

        Assertions.assertEquals(itemDtoOut, actualItemDto);
    }

    @Test
    void shouldThrowException_whenGettingNonexistentItem() {
        when(itemRepository.findById(id)).thenReturn(Optional.empty());

        Assertions.assertThrows(EntityNotFoundException.class, () -> itemService.getItemById(id, id));
    }

    @Test
    void shouldReturnEmptyList_whenOwnerHasNoItems() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(itemRepository.findAllByOwnerId(anyLong(), any())).thenReturn(Collections.emptyList());

        List<ItemDtoOut> targetItems = itemService.getItemsByOwner(0, 10, id);

        Assertions.assertTrue(targetItems.isEmpty());
    }

    @Test
    void shouldReturnMultipleItems_whenSearchTextMatchesMultipleItems() {
        when(itemRepository.search(any(), any())).thenReturn(List.of(item, anotherItem));

        List<ItemDtoOut> targetItems = itemService.getItemBySearch(0, 10, "abc");

        Assertions.assertEquals(2, targetItems.size());
    }

    @Test
    void shouldThrowNotBookerException_whenCommentTextIsEmpty() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));

        Assertions.assertThrows(NotBookerException.class, () ->
                itemService.saveNewComment(id, new CommentDtoIn(""), id));
    }

    @Test
    void shouldSaveItemWithRequest_whenUserExistsAndRequestExists() {
        User user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(requestRepository.findById(itemRequest.getId())).thenReturn(Optional.of(itemRequest));

        Item item = new Item();
        item.setId(1L);
        item.setRequest(itemRequest);
        item.setOwner(user);

        when(itemRepository.save(any(Item.class))).thenReturn(item);

        itemDtoIn.setRequestId(itemRequest.getId());
        ItemDtoOut actualItemDto = itemService.saveNewItem(itemDtoIn, id);

        Assertions.assertEquals(ItemMapper.toDto(item), actualItemDto);
        Assertions.assertNotNull(item.getRequest());
    }

    @Test
    void shouldReturnItem_whenUserIsNotOwner() {
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));

        ItemDtoOut actualItemDto = itemService.getItemById(id, notOwnerId);
        ItemDtoOut expectedItemDto = ItemMapper.toDto(item);
        expectedItemDto.setComments(Collections.emptyList());

        Assertions.assertEquals(expectedItemDto, actualItemDto);
    }

    @Test
    void shouldReturnItemWithNoComments() {
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));
        when(commentRepository.findAllByItemId(id)).thenReturn(Collections.emptyList());

        ItemDtoOut actualItemDto = itemService.getItemById(id, notOwnerId);

        Assertions.assertTrue(actualItemDto.getComments().isEmpty());
    }

    @Test
    void shouldUpdateItemAvailability_whenChangingAvailability() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));

        itemDtoIn.setAvailable(true);
        ItemDtoOut actualItemDto = itemService.updateItem(id, itemDtoIn, id);

        Assertions.assertTrue(actualItemDto.getAvailable());
    }

    @Test
    void shouldReturnItemsFilteredByOwner() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(itemRepository.findAllByOwnerId(anyLong(), any())).thenReturn(List.of(item, anotherItem));

        List<ItemDtoOut> targetItems = itemService.getItemsByOwner(0, 10, id);

        Assertions.assertEquals(2, targetItems.size());
    }














}
