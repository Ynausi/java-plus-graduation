package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service")
public interface RequestClient {

    @GetMapping("/internal/requests/events/{eventId}/confirmed-count")
    Long getConfirmedCount(@PathVariable Long eventId);

    @PostMapping("/internal/requests/events/confirmed-counts")
    Map<Long, Long> getConfirmedCounts(@RequestBody List<Long> eventIds);
}