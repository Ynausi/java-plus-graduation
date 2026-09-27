package ru.practicum.controller.rating;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventReactionDto;
import ru.practicum.model.ReactionType;
import ru.practicum.service.event.EventReactionService;
import ru.practicum.service.event.EventService;

import java.util.List;

@RestController
@RequestMapping("/v1/{userId}/ratings")
@RequiredArgsConstructor
public class PrivateRatingController {

    private final EventReactionService eventReactionService;
    private final EventService eventService;

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
        return eventService.getFavoriteEvents(userId);
    }
}
