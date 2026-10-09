package ru.practicum.dto;

import lombok.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.practicum.dto.category.CategoryResponse;
import ru.practicum.model.EventState;
import ru.practicum.util.DateTimeFormatConstants;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventFullDto {
    private String annotation;
    private CategoryResponse category;
    private Long confirmedRequests;
    @JsonFormat(pattern = DateTimeFormatConstants.DATE_TIME_PATTERN)
    private LocalDateTime createdOn;
    private String description;
    @JsonFormat(pattern = DateTimeFormatConstants.DATE_TIME_PATTERN)
    private LocalDateTime eventDate;
    private Long id;
    private UserShortDto initiator;
    private LocationDto location;
    private Boolean paid;
    private Integer participantLimit;
    @JsonFormat(pattern = DateTimeFormatConstants.DATE_TIME_PATTERN)
    private LocalDateTime publishedOn;
    private Boolean requestModeration;
    private EventState state;
    private String title;
    private Integer rating;
    private Long views;
}