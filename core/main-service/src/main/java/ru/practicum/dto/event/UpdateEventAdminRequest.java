package ru.practicum.dto.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

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