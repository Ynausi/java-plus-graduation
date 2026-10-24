package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EventFullDto;
import ru.practicum.dto.EventReactionDto;
import ru.practicum.model.ReactionType;
import ru.practicum.service.EventReactionService;

import java.util.List;


@RestController
@RequestMapping("/v1/{userId}/ratings")
@RequiredArgsConstructor
public class PrivateRatingController {

    private final EventReactionService eventReactionService;

    @PostMapping("/{eventId}/{reaction}")
    @ResponseStatus(HttpStatus.CREATED)
    public EventReactionDto saveReaction(@PathVariable Long eventId,
                                         @PathVariable Long userId,
                                         @PathVariable ReactionType reaction) {
        return eventReactionService.addReaction(userId, eventId, reaction);
    }

    @DeleteMapping("/{eventId}/{reaction}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReaction(@PathVariable Long eventId,
                               @PathVariable Long userId,
                               @PathVariable ReactionType reaction) {
        eventReactionService.deleteReaction(userId, eventId, reaction);
    }

    @GetMapping
    public List<EventFullDto> getEventsUserLiked(@PathVariable Long userId) {
        return eventReactionService.getFavoriteEvents(userId);
    }
}
