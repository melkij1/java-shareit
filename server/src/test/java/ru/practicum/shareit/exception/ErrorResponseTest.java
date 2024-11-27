package ru.practicum.shareit.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ErrorResponseTest {
    @Test
    public void testErrorResponseConstructorAndGetter() {

        String expectedError = "Some error occurred";

        ErrorResponse errorResponse = new ErrorResponse(expectedError);

        assertEquals(expectedError, errorResponse.getError(), "Error message should match the expected error");
    }
}
