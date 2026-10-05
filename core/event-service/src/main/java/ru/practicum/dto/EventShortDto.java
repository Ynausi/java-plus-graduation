package ru.practicum.dto;

import lombok.*;
import ru.practicum.dto.category.CategoryResponse;

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
    private Long initiator;
    private Long confirmedRequests;
    private Long views;
    private Boolean paid;
    private Integer rating;
    private LocalDateTime eventDate;
}