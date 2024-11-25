package ru.practicum.shareit.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDtoIn;
import ru.practicum.shareit.booking.dto.BookingDtoOut;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.exception.ItemIsNotAvailableException;
import ru.practicum.shareit.exception.NotAvailableToBookOwnItemsException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
public class BookingServiceIntegrationTest {

    @Autowired
    private BookingService bookingService; // Ваш сервис бронирования

    @Autowired
    private UserRepository  userRepository; // Репозиторий пользователей

    @Autowired
    private ItemRepository itemRepository; // Репозиторий предметов

    private User owner;
    private User booker;
    private Item availableItem;
    private Item unavailableItem;

    @BeforeEach
    public void setUp() {
        // Создаем и сохраняем владельца и бронирующего пользователя
        owner = new User();
        owner.setName("Owner");
        owner.setEmail("owner@example.com");
        userRepository.save(owner);

        booker = new User();
        booker.setName("Booker");
        booker.setEmail("booker@example.com");
        userRepository.save(booker);

        // Создаем доступный предмет
        availableItem = new Item();
        availableItem.setName("Available Item");
        availableItem.setDescription("This item is available for booking.");
        availableItem.setOwner(owner);
        availableItem.setAvailable(true);
        itemRepository.save(availableItem);

        // Создаем недоступный предмет
        unavailableItem = new Item();
        unavailableItem.setName("Unavailable Item");
        unavailableItem.setDescription("This item is not available for booking.");
        unavailableItem.setOwner(owner);
        unavailableItem.setAvailable(false);
        itemRepository.save(unavailableItem);
    }

    @Test
    public void testSaveBookingSuccessfully() {
        // Создаем DTO для бронирования
        BookingDtoIn bookingDtoIn = new BookingDtoIn(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), availableItem.getId());

        // Сохраняем бронирование
        BookingDtoOut savedBooking = bookingService.save(bookingDtoIn, booker.getId());

        // Проверяем, что бронирование было успешно создано
        assertThat(savedBooking).isNotNull();
        assertThat(savedBooking.getItem().getId()).isEqualTo(availableItem.getId()); // Проверяем идентификатор предмета
        assertThat(savedBooking.getBooker().getId()).isEqualTo(booker.getId()); // Проверяем идентификатор бронирующего
    }


    @Test
    public void testSaveBookingThrowsExceptionWhenItemNotAvailable() {
        // Создаем DTO для бронирования недоступного предмета
        BookingDtoIn bookingDtoIn = new BookingDtoIn(LocalDateTime.now().plusDays(1),LocalDateTime.now().plusDays(2),unavailableItem.getId());


        // Проверяем, что выбрасывается исключение
        assertThatThrownBy(() -> bookingService.save(bookingDtoIn, booker.getId()))
                .isInstanceOf(ItemIsNotAvailableException.class)
                .hasMessage("Вещь недоступна для брони");
    }

    @Test
    public void testSaveBookingThrowsExceptionWhenUserBooksOwnItem() {
        // Создаем DTO для бронирования собственного предмета
        BookingDtoIn bookingDtoIn = new BookingDtoIn(LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(1),availableItem.getId());


        // Проверяем, что выбрасывается исключение
        assertThatThrownBy(() -> bookingService.save(bookingDtoIn, owner.getId()))
                .isInstanceOf(NotAvailableToBookOwnItemsException.class)
                .hasMessage("Функция бронировать собственную вещь отсутствует");
    }
}