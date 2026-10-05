package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/v1/ratings")
@RequiredArgsConstructor
public class PublicRatingController {


    /*@GetMapping
    public List<EventShortDto> getSortedEvents(@RequestParam(defaultValue = "DESC") String sort,
                                               @RequestParam(defaultValue = "10") Integer size) {
        return eventService.getTopEventsByRating(size, sort);
    }*/
}