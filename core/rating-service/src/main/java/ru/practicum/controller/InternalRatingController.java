package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.service.EventReactionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/ratings")
@RequiredArgsConstructor
public class InternalRatingController {

    private final EventReactionService eventReactionService;

    @PostMapping("/events")
    public Map<Long,Integer> getRatings(@RequestBody List<Long> eventIds) {
        return eventReactionService.getRatings(eventIds);
    }
}
