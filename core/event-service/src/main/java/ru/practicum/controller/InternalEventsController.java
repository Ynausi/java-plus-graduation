package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EventForRequestDto;
import ru.practicum.dto.EventFullDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.service.event.EventService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventsController {

    private final EventService eventService;

    @GetMapping("/{eventId}")
    public EventForRequestDto getEvent(@PathVariable Long eventId) {
        return eventService.getEventById(eventId);
    }

    @PostMapping("/full/by-ids")
    public List<EventFullDto> getFullEventsByIds(@RequestBody List<Long> eventIds) {
        return eventService.getFullEventsByIds(eventIds);
    }

    @PostMapping("/short/by-ids")
    public List<EventShortDto> getShortEventsByIds(@RequestBody List<Long> eventIds) {
        return eventService.getShortEventsByIds(eventIds);
    }

    @PostMapping("/owners/by-initiators")
    public Map<Long, Long> getEventOwnersByInitiatorIds(@RequestBody List<Long> userIds) {
        return eventService.getEventOwnersByInitiatorIds(userIds);
    }
}
