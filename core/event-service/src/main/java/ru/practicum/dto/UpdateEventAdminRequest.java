package ru.practicum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;
import ru.practicum.util.DateTimeFormatConstants;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventAdminRequest {

    @Size(min = 3, max = 120)
    private String title;

    @Size(min = 20, max = 2000)
    private String annotation;

    @Size(min = 20, max = 7000)
    private String description;

    @JsonFormat(pattern = DateTimeFormatConstants.DATE_TIME_PATTERN)
    private LocalDateTime eventDate;

    @Valid
    private LocationDto location;

    @PositiveOrZero
    private Integer participantLimit;

    private Long category;
    private Boolean paid;
    private Boolean requestModeration;
    private AdminStateAction stateAction;
}