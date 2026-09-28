package ru.practicum.controller.rating;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.service.event.EventService;

import java.util.List;

@RestController
@RequestMapping("/v1/ratings")
@RequiredArgsConstructor
public class PublicRatingController {

    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> getSortedEvents(@RequestParam(defaultValue = "DESC") String sort,
                                               @RequestParam(defaultValue = "10") Integer size) {
        return eventService.getTopEventsByRating(size, sort);
    }
}