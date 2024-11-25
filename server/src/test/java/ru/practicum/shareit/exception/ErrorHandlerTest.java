package ru.practicum.shareit.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ExceptionHandlerTest {

    private final ErrorHandler exceptionHandler = new ErrorHandler();


    @Test
    void shouldReturnBadRequest_whenItemIsNotAvailableException() {
        ItemIsNotAvailableException exception = new ItemIsNotAvailableException("Item not available");
        ErrorResponse response = exceptionHandler.validateException(exception);
        assertEquals("Item not available", response.getError());
    }

    @Test
    void shouldReturnBadRequest_whenNotBookerException() {
        NotBookerException exception = new NotBookerException("Not a booker");
        ErrorResponse response = exceptionHandler.validateException(exception);

        assertEquals("Not a booker", response.getError());
    }

    @Test
    void shouldReturnBadRequest_whenUnsupportedStatusException() {
        UnsupportedStatusException exception = new UnsupportedStatusException("Unsupported status");
        ErrorResponse response = exceptionHandler.validateException(exception);

        assertEquals("Unsupported status", response.getError());
    }

    @Test
    void shouldReturnNotFound_whenEntityNotFoundException() {
        EntityNotFoundException exception = new EntityNotFoundException("Entity not found");
        ErrorResponse response = exceptionHandler.entityNotFoundException(exception);

        assertEquals("Entity not found", response.getError());
    }


    @Test
    void shouldReturnConflict_whenNotUniqueEmailException() {
        NotUniqueEmailException exception = new NotUniqueEmailException("Email already exists");
        ErrorResponse response = exceptionHandler.userNotUniqueEmailException(exception);

        assertEquals("Email already exists", response.getError());
    }

    @Test
    void shouldReturnForbidden_whenNotOwnerException() {
        NotOwnerException exception = new NotOwnerException("Not the owner");
        ErrorResponse response = exceptionHandler.notOwnerException(exception);

        assertEquals("Not the owner", response.getError());
    }

    @Test
    void shouldReturnInternalServerError_whenThrowable() {
        Throwable exception = new Throwable("Unexpected error");
        ErrorResponse response = exceptionHandler.handleThrowable(exception);

        assertEquals("Произошла непредвиденная ошибка.", response.getError());
    }
}

