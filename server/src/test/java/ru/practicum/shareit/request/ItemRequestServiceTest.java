package ru.practicum.shareit.request;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import ru.practicum.shareit.exception.EntityNotFoundException;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.dto.ItemRequestDtoIn;
import ru.practicum.shareit.request.dto.ItemRequestDtoOut;
import ru.practicum.shareit.request.dto.ItemRequestMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemRequestServiceTest {

    @Mock
    private ItemRequestRepository requestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemRepository itemRepository;
    @InjectMocks
    private ItemRequestService requestService;

    private final User requestor = new User(2L, "user2", "user2@mail.ru");
    private final User user = new User(1L, "User", "user@mail.ru");
    private final ItemRequest request = new ItemRequest(1L, "description", requestor, LocalDateTime.now());
    private final ItemRequest requestSecond = new ItemRequest(2L, "2", user, LocalDateTime.now());
    private final Item item = new Item(1L, "item", "cool", true, user, request);
    private final Item itemSecond = new Item(2L, "i2", "2", true, requestor, requestSecond);

    @Test
    void shouldReturnSavedRequest_whenNewRequestIsCreated() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(requestor));
        when(requestRepository.save(any())).thenReturn(request);

        final ItemRequestDtoOut actualRequest = requestService.saveNewRequest(
                new ItemRequestDtoIn("description"), 2L);

        Assertions.assertEquals(ItemRequestMapper.toItemRequestDtoOut(request), actualRequest);
    }

    @Test
    void shouldReturnSavedRequests_whenRequestorIsFound() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(requestor));
        when(requestRepository.findAllByRequestorId(anyLong(), any())).thenReturn(List.of(request));
        when(itemRepository.findAllByRequestId(1L)).thenReturn(List.of(item));
        final ItemRequestDtoOut requestDtoOut = ItemRequestMapper.toItemRequestDtoOut(request);
        requestDtoOut.setItems(List.of(ItemMapper.toDto(item)));

        List<ItemRequestDtoOut> actualRequests = requestService.getRequestsByRequestor(2L);

        Assertions.assertEquals(List.of(requestDtoOut), actualRequests);
    }

    @Test
    void shouldThrowException_whenRequestorIsNotFound() {
        when((userRepository).findById(3L)).thenReturn(Optional.empty());

        Assertions.assertThrows(EntityNotFoundException.class, () ->
                requestService.getRequestsByRequestor(3L));
    }

    @Test
    void shouldReturnRequestById_whenUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(requestRepository.findById(anyLong())).thenReturn(Optional.of(request));
        when(itemRepository.findAllByRequestId(1L)).thenReturn(List.of(item));
        final ItemRequestDtoOut requestDto = ItemRequestMapper.toItemRequestDtoOut(request);
        requestDto.setItems(List.of(ItemMapper.toDto(item)));

        ItemRequestDtoOut actualRequest = requestService.getRequestById(1L, 1L);

        Assertions.assertEquals(requestDto, actualRequest);
    }

    @Test
    void shouldThrowException_whenUserNotFoundOnSaveRequest() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        Assertions.assertThrows(EntityNotFoundException.class, () ->
                requestService.saveNewRequest(new ItemRequestDtoIn("description"), 2L));
    }

    @Test
    void shouldReturnEmptyList_whenNoRequestsFoundForRequestor() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(requestor));
        when(requestRepository.findAllByRequestorId(anyLong(), any())).thenReturn(Collections.emptyList());

        List<ItemRequestDtoOut> actualRequests = requestService.getRequestsByRequestor(2L);

        Assertions.assertTrue(actualRequests.isEmpty());
    }

    @Test
    void shouldThrowException_whenUserNotFoundOnGetAllRequests() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(EntityNotFoundException.class, () ->
                requestService.getAllRequests(0, 10, 1L));
    }

    @Test
    void shouldThrowException_whenRequestNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(requestRepository.findById(1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(EntityNotFoundException.class, () ->
                requestService.getRequestById(1L, 1L));
    }

    @Test
    void shouldReturnEmptyList_whenNoRequestsFoundInPagination() {
        long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(requestRepository.findAllByRequestorIdIsNot(eq(userId), any(Pageable.class))).thenReturn(Collections.emptyList());

        List<ItemRequestDtoOut> actualRequests = requestService.getAllRequests(0, 10, userId);

        Assertions.assertTrue(actualRequests.isEmpty(), "Expected an empty list when no requests are found");
    }
}
