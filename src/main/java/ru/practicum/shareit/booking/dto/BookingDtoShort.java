package ru.practicum.shareit.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.practicum.shareit.enums.BookingStatusEnum;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class BookingDtoShort {
    private long id;
    private LocalDateTime start;
    private LocalDateTime end;
    private BookingStatusEnum status;
    private long bookerId;
}