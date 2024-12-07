package ru.practicum.shareit.booking.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingDtoOut;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.enums.BookingStateEnum;
import ru.practicum.shareit.enums.BookingStatusEnum;
import ru.practicum.shareit.exception.*;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.data.domain.Sort.Direction.DESC;

@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    public BookingDtoOut save(BookingDto bookingDtoIn, long userId) {
        User booker = getUser(userId);
        Item item = getItem(bookingDtoIn.getItemId());
        if (!item.getAvailable()) {
            throw new ItemIsNotAvailableException("Вещь недоступна для брони");
        }
        if (booker.getId() == item.getOwner().getId()) {
            throw new NotAvailableToBookOwnItemsException("Функция бронировать собственную вещь отсутствует");
        }
        if (!bookingDtoIn.getEnd().isAfter(bookingDtoIn.getStart()) ||
                bookingDtoIn.getStart().isBefore(LocalDateTime.now())) {
            throw new WrongDatesException("Дата начала бронирования должна быть раньше даты возврата");
        }
        Booking booking = new Booking();
        booking.setItem(item);
        booking.setBooker(booker);
        bookingRepository.save(BookingMapper.toBooking(bookingDtoIn, booking));
        log.info("Бронирование с идентификатором {} создано", booking.getId());
        return BookingMapper.toBookingDtoOut(booking);
    }

    @Override
    public BookingDtoOut approve(long bookingId, Boolean isApproved, long userId) {
        Booking booking = getById(bookingId);
        Item item = getItem(booking.getItem().getId());

        if (booking.getStatus() != BookingStatusEnum.WAITING) {
            throw new ItemIsNotAvailableException("Вещь уже забронирована");
        }
        if (booking.getItem().getOwner().getId() != userId) {
            throw new IllegalViewAndUpdateException("Подтвердить бронирование может только собственник вещи");
        }
        BookingStatusEnum newBookingStatus = isApproved ? BookingStatusEnum.APPROVED : BookingStatusEnum.REJECTED;
        booking.setStatus(newBookingStatus);
        log.info("Бронирование с идентификатором {} обновлено", booking.getId());
        return BookingMapper.toBookingDtoOut(booking);

    }

    @Transactional(readOnly = true)
    @Override
    public BookingDtoOut getBookingById(long bookingId, long userId) {
        log.info("Получение бронирования по идентификатору {}", bookingId);
        Booking booking = getById(bookingId);
        User booker = booking.getBooker();
        User owner = getUser(booking.getItem().getOwner().getId());
        if (booker.getId() != userId && owner.getId() != userId) {
            throw new IllegalViewAndUpdateException("Только автор или владелец может просматривать данное броинрование");
        }
        return BookingMapper.toBookingDtoOut(booking);
    }

    @Transactional(readOnly = true)
    @Override
    public List<BookingDtoOut> getAllByBooker(String state, long bookerId) {
        User booker = getUser(bookerId);
        List<Booking> bookings;
        BookingStateEnum bookingState;
        try {
            bookingState = BookingStateEnum.valueOf(state);
        } catch (IllegalArgumentException e) {
            throw new UnsupportedStatusException("Unknown state: UNSUPPORTED_STATUS");
        }
        switch (bookingState) {
            case ALL:
                bookings = bookingRepository.findAllByBookerId(booker.getId(), Sort.by(DESC, "start"));
                break;
            case CURRENT:
                bookings = bookingRepository.findAllByBookerIdAndStateCurrent(booker.getId(),
                        Sort.by(Sort.Direction.DESC, "start"));
                break;
            case PAST:
                bookings = bookingRepository.findAllByBookerIdAndStatePast(booker.getId(),
                        Sort.by(Sort.Direction.DESC, "start"));
                break;
            case FUTURE:
                bookings = bookingRepository.findAllByBookerIdAndStateFuture(booker.getId(),
                        Sort.by(Sort.Direction.DESC, "start"));
                break;
            case WAITING:
                bookings = bookingRepository.findAllByBookerIdAndStatus(booker.getId(),
                        BookingStatusEnum.WAITING, Sort.by(Sort.Direction.DESC, "start"));
                break;
            case REJECTED:
                bookings = bookingRepository.findAllByBookerIdAndStatus(booker.getId(),
                        BookingStatusEnum.REJECTED, Sort.by(DESC, "end"));
                break;
            default:
                throw new UnsupportedStatusException("Unknown state: UNSUPPORTED_STATUS");
        }
        return bookings.stream().map(BookingMapper::toBookingDtoOut).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<BookingDtoOut> getAllByOwner(long ownerId, String state) {
        User owner = getUser(ownerId);
        List<Booking> bookings;
        BookingStateEnum bookingState;
        try {
            bookingState = BookingStateEnum.valueOf(state);
        } catch (IllegalArgumentException e) {
            throw new UnsupportedStatusException("Unknown state: UNSUPPORTED_STATUS");
        }
        switch (bookingState) {
            case ALL:
                bookings = bookingRepository.findAllByOwnerId(owner.getId(),
                        Sort.by(Sort.Direction.DESC, "start"));
                break;
            case CURRENT:
                bookings = bookingRepository.findAllByOwnerIdAndStateCurrent(owner.getId(),
                        Sort.by(Sort.Direction.DESC, "start"));
                break;
            case PAST:
                bookings = bookingRepository.findAllByOwnerIdAndStatePast(owner.getId(),
                        Sort.by(Sort.Direction.DESC, "start"));
                break;
            case FUTURE:
                bookings = bookingRepository.findAllByOwnerIdAndStateFuture(owner.getId(),
                        Sort.by(Sort.Direction.DESC, "start"));
                break;
            case WAITING:
                bookings = bookingRepository.findAllByOwnerIdAndStatus(owner.getId(),
                        BookingStatusEnum.WAITING, Sort.by(Sort.Direction.DESC, "start"));
                break;
            case REJECTED:
                bookings = bookingRepository.findAllByOwnerIdAndStatus(owner.getId(),
                        BookingStatusEnum.REJECTED, Sort.by(Sort.Direction.DESC, "start"));
                break;
            default:
                throw new UnsupportedStatusException("Unknown state: UNSUPPORTED_STATUS");
        }
        return bookings.stream().map(BookingMapper::toBookingDtoOut).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Booking getById(long bookingId) {
        log.info("Получение бронирования по идентификатору {}", bookingId);
        return bookingRepository.findById(bookingId).orElseThrow(() ->
                new EntityNotFoundException(String.format("Объект класса %s не найден", Booking.class)));
    }

    private User getUser(long userId) {
        return userRepository.findById(userId).orElseThrow(() ->
                new EntityNotFoundException(String.format("Объект класса %s не найден", User.class)));
    }

    private Item getItem(long itemId) {
        return itemRepository.findById(itemId).orElseThrow(() ->
                new EntityNotFoundException(String.format("Объект класса %s не найден", Item.class)));
    }
}
