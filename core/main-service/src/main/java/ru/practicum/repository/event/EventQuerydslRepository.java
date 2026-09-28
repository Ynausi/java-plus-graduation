package ru.practicum.repository.event;

import org.springframework.data.domain.Pageable;
import ru.practicum.dto.event.EventSearchFilterAdmin;
import ru.practicum.dto.event.EventSearchFilterPublic;
import ru.practicum.model.Event;

import java.util.List;

public interface EventQuerydslRepository {
    List<Event> searchAdmin(EventSearchFilterAdmin filter, Pageable pageable);

    List<Event> searchPublic(EventSearchFilterPublic filter, Pageable pageable);
}