package ru.practicum.shareit.request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.shareit.utils.Create;
import ru.practicum.shareit.utils.Update;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestDto {

    private long id;

    @NotBlank(groups = {Create.class, Update.class})
    @Size(max = 1000, groups = {Create.class, Update.class})
    private String description;
}