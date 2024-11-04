package ru.practicum.shareit.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;
import ru.practicum.shareit.utils.Create;

@Data
@Builder
public class UserDto {
    private long id;
    @NotBlank(groups = {Create.class})
    private String name;
    @NotEmpty(groups = {Create.class})
    @Email(groups = {Create.class, UserDto.class})
    private String email;
}
