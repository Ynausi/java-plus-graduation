package ru.practicum.dto.event;

import ru.practicum.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

public record EventSearchFilterAdmin(
        List<Long> users,
        List<EventState> states,
        List<Long> categories,
        LocalDateTime rangeStart,
        LocalDateTime rangeEnd
) {
}