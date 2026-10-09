package ru.practicum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import ru.practicum.dto.category.CategoryResponse;
import ru.practicum.util.DateTimeFormatConstants;

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
    @JsonFormat(pattern = DateTimeFormatConstants.DATE_TIME_PATTERN)
    private LocalDateTime eventDate;
}