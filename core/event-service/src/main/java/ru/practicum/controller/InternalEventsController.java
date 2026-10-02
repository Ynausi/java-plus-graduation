package ru.practicum.controller;

import jakarta.ws.rs.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.EventForRequestDto;
import ru.practicum.service.EventService;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventsController {

    private final EventService eventService;

    @GetMapping("/{eventId}")
    public EventForRequestDto getEvent(@PathVariable Long eventId) {
        return eventService.getEventById(eventId);
    }
}
