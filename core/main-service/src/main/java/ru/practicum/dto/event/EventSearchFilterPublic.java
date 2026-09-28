package ru.practicum.dto.event;

import java.time.LocalDateTime;
import java.util.List;

public record EventSearchFilterPublic(
        String text,
        List<Long> categories,
        List<Long> users,
        Boolean paid,
        LocalDateTime rangeStart,
        LocalDateTime rangeEnd,
        Boolean onlyAvailable,
        PublicEventSort sort
) {
}