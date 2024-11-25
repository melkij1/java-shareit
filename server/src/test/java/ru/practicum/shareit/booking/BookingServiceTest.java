package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.dto.BookingDtoIn;
import ru.practicum.shareit.booking.dto.BookingDtoOut;
import ru.practicum.shareit.booking.dto.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.booking.service.BookingServiceImpl;
import ru.practicum.shareit.enums.BookingStatusEnum;
import ru.practicum.shareit.exception.*;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.user.service.UserServiceImpl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private BookingServiceImpl bookingService;

    @InjectMocks
    private UserServiceImpl userService;

    private final User user = new User(1L, "User", "user@mail.ru");
    private final UserDto owner = new UserDto(1L, "Owner", "owner@example.com");
    private final User booker = new User(2L, "user2", "user2@mail.ru");
    private final UserDto bookerDto = new UserDto(2L, "user2", "user2@mail.ru");
    private final Item item = new Item(1L, "item", "cool", true, user, null);
    private final Booking booking = new Booking(1L,
            LocalDateTime.of(2023, 7, 1, 12, 12, 12),
            LocalDateTime.of(2023, 7, 30, 12, 12, 12),
            item, booker, BookingStatusEnum.WAITING);
    private final BookingDtoIn bookingDtoIn = new BookingDtoIn(
            LocalDateTime.of(2023, 7, 1, 12, 12, 12),
            LocalDateTime.of(2023, 7, 30, 12, 12, 12), 1L);
    private final BookingDtoIn bookingDtoWrongItem = new BookingDtoIn(
            LocalDateTime.of(2023, 7, 1, 12, 12, 12),
            LocalDateTime.of(2023, 7, 30, 12, 12, 12), 2L);


    @Test
    void shouldThrowException_whenUser_DoesNotExist() {
        when((userRepository).findById(3L)).thenReturn(Optional.empty());

        Assertions.assertThrows(EntityNotFoundException.class, () ->
                bookingService.save(bookingDtoIn, 3L));
    }

    @Test
    void shouldThrowException_whenItemDoesNotExist() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when((itemRepository).findById(2L)).thenReturn(Optional.empty());

        Assertions.assertThrows(EntityNotFoundException.class, () ->
                bookingService.save(bookingDtoWrongItem, 2L));
    }

    @Test
    void shouldThrowExceptionItems_whenIsNotAvailable() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        item.setAvailable(false);

        Assertions.assertThrows(ItemIsNotAvailableException.class, () ->
                bookingService.save(bookingDtoIn, 2L));
    }

    @Test
    void shouldThrowException_whenBookerIsOwnerOfItem() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Assertions.assertThrows(NotAvailableToBookOwnItemsException.class, () ->
                bookingService.save(bookingDtoIn, 1L));
    }

    @Test
    void shouldThrowException_whenOwnerAttemptsToBookOwnItem() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Assertions.assertThrows(NotAvailableToBookOwnItemsException.class, () ->
                bookingService.save(bookingDtoIn, 1L));
    }

    @Test
    void shouldApproveBooking_whenConditionsAreMet() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        BookingDtoOut actualBooking = bookingService.approve(1L, true, 1L);

        assertEquals(BookingStatusEnum.APPROVED, actualBooking.getStatus());
    }


    @Test
    void shouldThrowException_whenBookingDoesNotExist() {
        when((bookingRepository).findById(2L)).thenReturn(Optional.empty());

        Assertions.assertThrows(EntityNotFoundException.class, () ->
                bookingService.approve(2L, true, 1L));
    }

    @Test
    void shouldThrowException_whenItemIsAlreadyBooked() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        booking.setStatus(BookingStatusEnum.APPROVED);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Assertions.assertThrows(ItemIsNotAvailableException.class, () ->
                bookingService.approve(1L, true, 1L));
    }


    @Test
    void shouldReturnBooking_whenUser_IsOwner() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        BookingDtoOut actualBooking = bookingService.getBookingById(1L, 1L);

        assertEquals(BookingMapper.toBookingDtoOut(booking), actualBooking);
    }

    @Test
    void shouldThrowException_whenUserIsNeitherAuthorNorOwner() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Assertions.assertThrows(IllegalViewAndUpdateException.class, () ->
                bookingService.getBookingById(1L, 3L));
    }


    @Test
    void shouldReturnAllBookings_whenStateIsAll() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerId(anyLong(), any())).thenReturn(List.of(booking));

        List<BookingDtoOut> actualBookings = bookingService.getAllByBooker(0, 10, "ALL", 2L);

        assertEquals(List.of(BookingMapper.toBookingDtoOut(booking)), actualBookings);
    }

    @Test
    void shouldReturnCurrentBookings_whenStateIsCurrent() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdAndStateCurrent(anyLong(), any())).thenReturn(List.of(booking));

        List<BookingDtoOut> actualBookings = bookingService.getAllByBooker(0, 10, "CURRENT", 2L);

        assertEquals(List.of(BookingMapper.toBookingDtoOut(booking)), actualBookings);
    }

    @Test
    void shouldReturnPastBookings_whenStateIsPast() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdAndStatePast(anyLong(), any())).thenReturn(List.of(booking));

        List<BookingDtoOut> actualBookings = bookingService.getAllByBooker(0, 10, "PAST", 2L);

        assertEquals(List.of(BookingMapper.toBookingDtoOut(booking)), actualBookings);
    }

    @Test
    void shouldReturnFutureBookings_whenStateIsFuture() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdAndStateFuture(anyLong(), any())).thenReturn(List.of(booking));

        List<BookingDtoOut> actualBookings = bookingService.getAllByBooker(0, 10, "FUTURE", 2L);

        assertEquals(List.of(BookingMapper.toBookingDtoOut(booking)), actualBookings);
    }

    @Test
    void shouldReturnWaitingBookings_whenStateIsWaiting() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdAndStatus(anyLong(), any(), any())).thenReturn(List.of(booking));

        List<BookingDtoOut> actualBookings = bookingService.getAllByBooker(0, 10, "WAITING", 2L);

        assertEquals(List.of(BookingMapper.toBookingDtoOut(booking)), actualBookings);
    }

    @Test
    void shouldThrowException_whenStateIsUnsupported() {
        Assertions.assertThrows(UnsupportedStatusException.class, () ->
                bookingService.getAllByBooker(0, 10, "a", 2L));
    }

    @Test
    void shouldReturnAllBookingsForOwner_whenStateIsAll() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.findAllByOwnerId(anyLong(), any())).thenReturn(List.of(booking));

        List<BookingDtoOut> actualBookings = bookingService.getAllByOwner(0, 10, "ALL", 1L);

        assertEquals(List.of(BookingMapper.toBookingDtoOut(booking)), actualBookings);

    }

    @Test
    void shouldReturnCurrentBookingsForOwner_whenStateIsCurrent() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.findAllByOwnerIdAndStateCurrent(anyLong(), any())).thenReturn(List.of(booking));

        List<BookingDtoOut> actualBookings = bookingService.getAllByOwner(0, 10, "CURRENT", 1L);

        assertEquals(List.of(BookingMapper.toBookingDtoOut(booking)), actualBookings);
    }

    @Test
    void shouldReturnPastBookingsForOwner_whenStateIsPast() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.findAllByOwnerIdAndStatePast(anyLong(), any())).thenReturn(List.of(booking));

        List<BookingDtoOut> actualBookings = bookingService.getAllByOwner(0, 10, "PAST", 1L);

        assertEquals(List.of(BookingMapper.toBookingDtoOut(booking)), actualBookings);
    }

    @Test
    void shouldReturnFutureBookingsForOwner_whenStateIsFuture() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.findAllByOwnerIdAndStateFuture(anyLong(), any())).thenReturn(List.of(booking));

        List<BookingDtoOut> actualBookings = bookingService.getAllByOwner(0, 10, "FUTURE", 1L);

        assertEquals(List.of(BookingMapper.toBookingDtoOut(booking)), actualBookings);
    }

    @Test
    void shouldReturnWaitingBookingsForOwner_whenStateIsWaiting() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.findAllByOwnerIdAndStatus(anyLong(), any(), any())).thenReturn(List.of(booking));

        List<BookingDtoOut> actualBookings = bookingService.getAllByOwner(0, 10, "WAITING", 1L);

        assertEquals(List.of(BookingMapper.toBookingDtoOut(booking)), actualBookings);
    }

    @Test
    void shouldThrowException_whenUserIsNotOwnerOfItem() {
        lenient().when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        lenient().when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        lenient().when(userRepository.findById(2L)).thenReturn(Optional.of(booker));

        Assertions.assertThrows(IllegalViewAndUpdateException.class, () ->
                bookingService.approve(1L, true, 2L));
    }



    @Test
    void shouldThrowException_whenBookingDoesNotExistInGetById() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(EntityNotFoundException.class, () ->
                bookingService.getBookingById(1L, 2L));
    }

    @Test
    void shouldReturnEmptyList_whenNoBookingsForBooker() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerId(anyLong(), any())).thenReturn(Collections.emptyList());

        List<BookingDtoOut> actualBookings = bookingService.getAllByBooker(0, 10, "ALL", 2L);

        Assertions.assertTrue(actualBookings.isEmpty());
    }

    @Test
    void shouldReturnEmptyList_whenNoBookingsForOwner() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.findAllByOwnerId(anyLong(), any())).thenReturn(Collections.emptyList());

        List<BookingDtoOut> actualBookings = bookingService.getAllByOwner(0, 10, "ALL", 1L);

        Assertions.assertTrue(actualBookings.isEmpty());
    }

    @Test
    void shouldThrowException_whenStateIsInvalidInGetAllByBooker() {
        Assertions.assertThrows(UnsupportedStatusException.class, () ->
                bookingService.getAllByBooker(0, 10, "INVALID_STATE", 2L));
    }


    @Test
    void shouldThrowException_whenStateIsInvalidInGetAllByOwner() {
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Assertions.assertThrows(UnsupportedStatusException.class, () ->
                bookingService.getAllByOwner(0, 10, "INVALID_STATE", 1L));
    }


    @Test
    void shouldRejectBooking_whenConditionsAreMet() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        BookingDtoOut actualBooking = bookingService.approve(1L, false, 1L);

        assertEquals(BookingStatusEnum.REJECTED, actualBooking.getStatus());
    }


    @Test
    void shouldThrowException_whenGettingBookingWithInvalidId() {
        when(bookingRepository.findById(2L)).thenReturn(Optional.empty());

        Assertions.assertThrows(EntityNotFoundException.class, () ->
                bookingService.getBookingById(2L, 1L));
    }


    @Test
    void shouldReturnEmptyList_whenNoBookingsForOwnerInGetAllByOwner() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.findAllByOwnerId(anyLong(), any())).thenReturn(Collections.emptyList());

        List<BookingDtoOut> actualBookings = bookingService.getAllByOwner(0, 10, "ALL", 1L);

        Assertions.assertTrue(actualBookings.isEmpty());
    }


    @Test
    void shouldReturnEmptyList_whenNoBookingsForBookerInGetAllByBooker() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerId(anyLong(), any())).thenReturn(Collections.emptyList());

        List<BookingDtoOut> actualBookings = bookingService.getAllByBooker(0, 10, "ALL", 2L);

        Assertions.assertTrue(actualBookings.isEmpty());
    }

    @Test
    void shouldThrowException_whenGettingAllBookingsForBookerWithInvalidState() {
        Assertions.assertThrows(UnsupportedStatusException.class, () ->
                bookingService.getAllByBooker(0, 10, "INVALID_STATE", 2L));
    }

    @Test
    void shouldThrowException_whenGettingAllBookingsForOwnerWithInvalidState() {
        Assertions.assertThrows(UnsupportedStatusException.class, () ->
                bookingService.getAllByOwner(0, 10, "INVALID_STATE", 1L));
    }

    @Test
    void shouldThrowException_whenItemIsNotAvailable() {
        User user = new User(2L, "Test User", "test@example.com");
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        item.setAvailable(false);

        Assertions.assertThrows(ItemIsNotAvailableException.class, () ->
                bookingService.save(bookingDtoIn, 2L));
    }

    @Test
    void shouldThrowException_whenApprovingBookingByNonOwner() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Assertions.assertThrows(IllegalViewAndUpdateException.class, () ->
                bookingService.approve(1L, true, 2L));
    }

    @Test
    void shouldThrowException_whenBookingIsNotPending() {
        booking.setStatus(BookingStatusEnum.APPROVED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        lenient().when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Assertions.assertThrows(ItemIsNotAvailableException.class, () ->
                bookingService.approve(1L, true, 1L));
    }


    @Test
    void shouldThrowException_whenGettingBookingWithInvalidUser() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User(1L, "Valid User", "valid@example.com"))); // Valid user
        lenient().when(userRepository.findById(3L)).thenReturn(Optional.of(new User(3L, "Another User", "another@example.com")));

        Assertions.assertThrows(IllegalViewAndUpdateException.class, () ->
                bookingService.getBookingById(1L, 3L));
    }


    @Test
    void shouldReturnAllBookingsForOwner_whenStateIsRejected() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.findAllByOwnerIdAndStatus(anyLong(), any(), any())).thenReturn(List.of(booking));

        List<BookingDtoOut> actualBookings = bookingService.getAllByOwner(0, 10, "REJECTED", 1L);

        assertEquals(List.of(BookingMapper.toBookingDtoOut(booking)), actualBookings);
    }

    @Test
    void shouldThrowException_whenBookingIsNotFoundOnGetById() {
        when(bookingRepository.findById(2L)).thenReturn(Optional.empty());

        Assertions.assertThrows(EntityNotFoundException.class, () ->
                bookingService.getBookingById(2L, 1L));
    }

    @Test
    void shouldThrowException_whenNoBookingsForOwnerAndStateIsWaiting() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.findAllByOwnerIdAndStatus(anyLong(), any(), any())).thenReturn(Collections.emptyList());

        List<BookingDtoOut> actualBookings = bookingService.getAllByOwner(0, 10, "WAITING", 1L);

        Assertions.assertTrue(actualBookings.isEmpty());
    }

    @Test
    void shouldReturnEmptyList_whenNoBookingsForBookerWithStateRejected() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdAndStatus(anyLong(), any(), any())).thenReturn(Collections.emptyList());

        List<BookingDtoOut> actualBookings = bookingService.getAllByBooker(0, 10, "REJECTED", 2L);
        Assertions.assertTrue(actualBookings.isEmpty());
    }

    @Test
    void shouldThrowException_whenUserIsNotAvailable() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        Assertions.assertThrows(EntityNotFoundException.class, () ->
                bookingService.save(bookingDtoIn, 2L));
    }

    @Test
    void shouldThrowException_whenBookingAlreadyApproved() {
        booking.setStatus(BookingStatusEnum.APPROVED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Assertions.assertThrows(ItemIsNotAvailableException.class, () ->
                bookingService.approve(1L, true, 1L));
    }

    @Test
    void shouldThrowException_whenGettingBookingWithNegativeId() {
        Assertions.assertThrows(EntityNotFoundException.class, () ->
                bookingService.getBookingById(-1L, 1L));
    }

    @Test
    void shouldReturnEmptyList_whenNoBookingsForBookerWithStateWaiting() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdAndStatus(anyLong(), any(), any())).thenReturn(Collections.emptyList());

        List<BookingDtoOut> actualBookings = bookingService.getAllByBooker(0, 10, "WAITING", 2L);
        Assertions.assertTrue(actualBookings.isEmpty());
    }

    @Test
    void shouldReturnEmptyList_whenNoBookingsForOwnerWithStateRejected() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.findAllByOwnerIdAndStatus(anyLong(), any(), any())).thenReturn(Collections.emptyList());

        List<BookingDtoOut> actualBookings = bookingService.getAllByOwner(0, 10, "REJECTED", 1L);
        Assertions.assertTrue(actualBookings.isEmpty());
    }

    @Test
    void shouldThrowException_whenBookingStartDateIsInThePast() {
        bookingDtoIn.setStart(LocalDateTime.now().minusDays(1));

        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Assertions.assertThrows(InvalidBookingDateException.class, () ->
                bookingService.save(bookingDtoIn, 2L));
    }


    @Test
    void shouldThrowException_whenBookingEndDateIsBeforeStartDate() {
        bookingDtoIn.setStart(LocalDateTime.now().plusDays(2));
        bookingDtoIn.setEnd(LocalDateTime.now().plusDays(1));

        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Assertions.assertThrows(InvalidBookingDateException.class, () ->
                bookingService.save(bookingDtoIn, 2L));
    }

    @Test
    void shouldThrowException_whenBookingEndDateIsNull() {
        bookingDtoIn.setEnd(null);

        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Assertions.assertThrows(InvalidBookingDateException.class, () ->
                bookingService.save(bookingDtoIn, 2L));
    }

    @Test
    void shouldThrowException_whenApprovingAlreadyApprovedBooking() {
        booking.setStatus(BookingStatusEnum.APPROVED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Assertions.assertThrows(ItemIsNotAvailableException.class, () ->
                bookingService.approve(1L, true, 1L));
    }

    @Test
    void shouldThrowException_whenRejectingAlreadyRejectedBooking() {
        booking.setStatus(BookingStatusEnum.REJECTED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Assertions.assertThrows(ItemIsNotAvailableException.class, () ->
                bookingService.approve(1L, false, 1L));
    }

    @Test
    void shouldThrowException_whenBookingStartDateIsNull() {
        bookingDtoIn.setStart(null);

        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Assertions.assertThrows(InvalidBookingDateException.class, () ->
                bookingService.save(bookingDtoIn, 2L));
    }

    @Test
    void shouldThrowException_whenItemIsNotAvailableForBooking() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        item.setAvailable(false);

        Assertions.assertThrows(ItemIsNotAvailableException.class, () ->
                bookingService.save(bookingDtoIn, 2L));
    }


    @Test
    void shouldThrowException_whenGettingBookingByIdWithNegativeId() {
        Assertions.assertThrows(EntityNotFoundException.class, () ->
                bookingService.getBookingById(-1L, 1L));
    }

    @Test
    void shouldReturnEmptyList_whenNoBookingsForOwnerWithStateFuture() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.findAllByOwnerIdAndStateFuture(anyLong(), any())).thenReturn(Collections.emptyList());

        List<BookingDtoOut> actualBookings = bookingService.getAllByOwner(0, 10, "FUTURE", 1L);
        Assertions.assertTrue(actualBookings.isEmpty());
    }

    @Test
    void shouldReturnEmptyList_whenNoBookingsForBookerWithStateFuture() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findAllByBookerIdAndStateFuture(anyLong(), any())).thenReturn(Collections.emptyList());

        List<BookingDtoOut> actualBookings = bookingService.getAllByBooker(0, 10, "FUTURE", 2L);
        Assertions.assertTrue(actualBookings.isEmpty());
    }

    @Test
    void shouldThrowException_whenBookingIsNotPendingOnApprove() {
        booking.setStatus(BookingStatusEnum.REJECTED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Assertions.assertThrows(ItemIsNotAvailableException.class, () ->
                bookingService.approve(1L, true, 1L));
    }

    @Test
    void shouldThrowException_whenBookingIsNotPendingOnReject() {
        booking.setStatus(BookingStatusEnum.APPROVED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Assertions.assertThrows(ItemIsNotAvailableException.class, () ->
                bookingService.approve(1L, false, 1L));
    }

    @Test
    void shouldThrowException_whenApprovingBookingWithInvalidUser() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Assertions.assertThrows(IllegalViewAndUpdateException.class, () ->
                bookingService.approve(1L, true, 2L));
    }


    @Test
    void shouldThrowException_whenBookingStartDateIsAfterEndDate() {
        bookingDtoIn.setStart(LocalDateTime.now().plusDays(2));
        bookingDtoIn.setEnd(LocalDateTime.now().plusDays(1));

        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Assertions.assertThrows(InvalidBookingDateException.class, () ->
                bookingService.save(bookingDtoIn, 2L));
    }

    @Test
    void shouldThrowException_whenItemIsNotAvailableDuringBookingPeriod() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        LocalDateTime now = LocalDateTime.now();
        booking.setStart(now.plusDays(1));
        booking.setEnd(now.plusDays(3));
        item.setAvailable(false);

        Assertions.assertThrows(ItemIsNotAvailableException.class, () ->
                bookingService.save(bookingDtoIn, 2L));
    }

    @Test
    void testGetAllByBooker_UnsupportedState() {
        Exception exception = assertThrows(UnsupportedStatusException.class, () -> {
            bookingService.getAllByBooker(0, 10, "UNSUPPORTED_STATE", booker.getId());
        });
        assertEquals("Unknown state: UNSUPPORTED_STATUS", exception.getMessage());
    }
}
