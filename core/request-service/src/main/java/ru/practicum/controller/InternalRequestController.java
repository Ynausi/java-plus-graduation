package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.service.ParticipationRequestService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class InternalRequestController {

    private final ParticipationRequestService requestService;

    @GetMapping("/events/{eventId}/confirmed-count")
    public Long getConfirmedCount(@PathVariable Long eventId) {
        return requestService.getConfirmedCount(eventId);
    }

    @PostMapping("/events/confirmed-counts")
    public Map<Long, Long> getConfirmedCounts(
            @RequestBody List<Long> eventIds) {

        return requestService.getConfirmedCounts(eventIds);
    }

    @GetMapping("/users/{userId}/events/{eventId}/confirmed")
    public boolean isConfirmedParticipant(
            @PathVariable Long userId,
            @PathVariable Long eventId) {

        return requestService.isConfirmedParticipant(userId, eventId);
    }
}
