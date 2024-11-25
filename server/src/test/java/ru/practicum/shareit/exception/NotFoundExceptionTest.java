package ru.practicum.shareit.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NotFoundExceptionTest {
    @Test
    public void testNotFoundExceptionMessage() {
        // Arrange
        String expectedMessage = "Resource not found";

        // Act
        NotFoundException exception = new NotFoundException(expectedMessage);

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }
}
