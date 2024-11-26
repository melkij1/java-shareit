package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.practicum.shareit.utils.Create;
import ru.practicum.shareit.utils.Update;

@Data
public class CommentDto {
    @Size(max = 1000, groups = {Create.class, Update.class})
    @NotBlank(groups = {Create.class})
    private String text;
}
