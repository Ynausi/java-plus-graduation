package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.EventForRequestDto;
import ru.practicum.dto.EventFullDto;
import ru.practicum.dto.EventShortDto;

import java.util.List;
import java.util.Map;

@FeignClient(name = "event-service")
public interface EventClient {
    @GetMapping("/internal/events/{eventId}")
    EventForRequestDto getEvent(@PathVariable Long eventId);

    @PostMapping("/internal/events/full/by-ids")
    List<EventFullDto> getFullEventsByIds(@RequestBody List<Long> eventIds);

    @PostMapping("/internal/events/short/by-ids")
    List<EventShortDto> getShortEventsByIds(@RequestBody List<Long> eventIds);

    @PostMapping("/internal/events/owners/by-initiators")
    Map<Long, Long> getEventOwnersByInitiatorIds(@RequestBody List<Long> userIds);
}
