package ru.practicum.dto.event;

import lombok.*;
import ru.practicum.dto.category.CategoryResponse;
import ru.practicum.dto.users.UserShortDto;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventShortDto {
    private Long id;
    private String title;
    private String annotation;
    private CategoryResponse category;
    private UserShortDto initiator;
    private Long confirmedRequests;
    private Long views;
    private Boolean paid;
    private Integer rating;
    private LocalDateTime eventDate;
}