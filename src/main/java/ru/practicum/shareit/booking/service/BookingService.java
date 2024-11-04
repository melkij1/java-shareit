package ru.practicum.shareit.booking.service;

import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingDtoOut;

import java.util.List;

public interface BookingService {
    BookingDtoOut save(BookingDto bookingDto, long userId);

    BookingDtoOut approve(long bookingId, Boolean isApproved, long userId);

    BookingDtoOut getBookingById(long bookingId, long userId);

    List<BookingDtoOut> getAllByBooker(String subState, long bookerId);

    List<BookingDtoOut> getAllByOwner(long ownerId, String state);
}
