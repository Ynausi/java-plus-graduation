package ru.practicum.controller;

import jakarta.ws.rs.Path;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.EventForRequestDto;

@RestController
@RequestMapping("/internal/event")
public class InternalEventsController {

    @GetMapping("/{eventId}")
    public EventForRequestDto getEvent(@PathVariable Long eventId) {
        return
    }
}
