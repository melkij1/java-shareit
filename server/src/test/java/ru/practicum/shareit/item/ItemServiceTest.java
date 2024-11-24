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
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.item.service.ItemServiceImpl;
import ru.practicum.shareit.user.dto.UserDtoShort;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
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
    @InjectMocks
    private ItemServiceImpl itemService;

    private final long id = 1L;
    private final User user = new User(id, "User", "user@mail.ru");
    private final User notOwner = new User(2L, "User2", "user2@mail.ru");
    private final ItemDtoIn itemDtoIn = new ItemDtoIn("item", "cool item", true, null);
    private final ItemDtoOut itemDtoOut = new ItemDtoOut(id, "item", "cool item", true,
            new UserDtoShort(id, "User"));
    private final Item item = new Item(id, "item", "cool item", true, user, null);
    private final CommentDtoOut commentDto = new CommentDtoOut(id, "abc", "User",
            LocalDateTime.of(2023, 7, 1, 12, 12, 12));
    private final Comment comment = new Comment(id, "abc", item, user,
            LocalDateTime.of(2023, 7, 1, 12, 12, 12));
    private final Booking booking = new Booking(id, null, null, item, user, BookingStatusEnum.WAITING);

    @Test
    void shouldReturnSavedItem_whenUser() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(itemRepository.save(any())).thenReturn(item);

        ItemDtoOut actualItemDto = itemService.saveNewItem(itemDtoIn, id);

        Assertions.assertEquals(ItemMapper.toDto(item), actualItemDto);
        Assertions.assertNull(item.getRequest());
    }

    @Test
    void shouldThrowException_whenUser() {
        when((userRepository).findById(2L)).thenReturn(Optional.empty());

        Assertions.assertThrows(EntityNotFoundException.class, () -> itemService.saveNewItem(itemDtoIn, 2L));
    }

    @Test
    void shouldThrowException_whenItemNameIsMissing() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        doThrow(DataIntegrityViolationException.class).when(itemRepository).save(any(Item.class));

        Assertions.assertThrows(DataIntegrityViolationException.class, () -> itemService.saveNewItem(itemDtoIn, id));
    }

    @Test
    void shouldReturnUpdatedItem_whenUser() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));

        ItemDtoOut actualItemDto = itemService.updateItem(id, itemDtoIn, id);

        Assertions.assertEquals(itemDtoOut, actualItemDto);
    }

    @Test
    void shouldThrowException_whenUserIsNotOwner() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(notOwner));
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));

        Assertions.assertThrows(NotOwnerException.class, () -> itemService.updateItem(id, itemDtoIn, 2L));
    }

    @Test
    void shouldReturnItemById_whenItemIsFound() {
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
    void shouldThrowException_whenItemIsNotFound() {
        when((itemRepository).findById(2L)).thenReturn(Optional.empty());

        Assertions.assertThrows(EntityNotFoundException.class, () -> itemService.getItemById(2L, id));
    }

    @Test
    void getItemsByOwner_CorrectArgumentsForPaging_thenReturnItems() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(itemRepository.findAllByOwnerId(anyLong(), any())).thenReturn(List.of(item));

        List<ItemDtoOut> targetItems = itemService.getItemsByOwner(0, 10, id);

        Assertions.assertNotNull(targetItems);
        Assertions.assertEquals(1, targetItems.size());
        verify(itemRepository, times(1))
                .findAllByOwnerId(anyLong(), any());
    }

    @Test
    void shouldReturnItemsByOwner_whenPagingParametersAreCorrect() {
        when(itemRepository.search(any(), any())).thenReturn(List.of(item));

        List<ItemDtoOut> targetItems = itemService.getItemBySearch(0, 10, "abc");

        Assertions.assertNotNull(targetItems);
        Assertions.assertEquals(1, targetItems.size());
        verify(itemRepository, times(1))
                .search(any(), any());
    }

    @Test
    void shouldReturnItems_whenSearchTextIsNotBlank() {
        List<ItemDtoOut> targetItems = itemService.getItemBySearch(0, 10, "");

        Assertions.assertTrue(targetItems.isEmpty());
        Assertions.assertEquals(0, targetItems.size());
        verify(itemRepository, never()).search(any(), any());
    }

    @Test
    void shouldReturnEmptyList_whenSearchTextIsBlank() {
        when(bookingRepository.existsByBookerIdAndItemIdAndEndBefore(anyLong(), anyLong(), any()))
                .thenReturn(true);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(commentRepository.save(any())).thenReturn(comment);
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));

        CommentDtoOut actualComment = itemService.saveNewComment(id, new CommentDtoIn("abc"), id);

        Assertions.assertEquals(commentDto, actualComment);
    }

}
