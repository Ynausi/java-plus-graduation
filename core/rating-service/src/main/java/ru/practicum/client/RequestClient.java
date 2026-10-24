package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "request-service")
public interface RequestClient {

    @GetMapping("/internal/requests/users/{userId}/events/{eventId}/confirmed")
    boolean isConfirmedParticipant(@PathVariable Long userId, @PathVariable Long eventId);
}
