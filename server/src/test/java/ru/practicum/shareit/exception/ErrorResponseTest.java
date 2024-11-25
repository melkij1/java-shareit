package ru.practicum.shareit.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ErrorResponseTest {
    @Test
    public void testErrorResponseConstructorAndGetter() {
        // Arrange
        String expectedError = "Some error occurred";

        // Act
        ErrorResponse errorResponse = new ErrorResponse(expectedError);

        // Assert
        assertEquals(expectedError, errorResponse.getError(), "Error message should match the expected error");
    }
}
