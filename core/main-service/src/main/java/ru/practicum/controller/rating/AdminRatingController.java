package ru.practicum.controller.rating;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.users.UserRatingStatsDto;
import ru.practicum.dto.users.UserShortDto;
import ru.practicum.model.ReactionType;
import ru.practicum.service.event.EventReactionService;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/ratings")
@RequiredArgsConstructor
public class AdminRatingController {

    private final EventReactionService eventReactionService;

    @GetMapping("/by-events")
    public List<UserShortDto> getUsersByReactionOrByEventIds(
            @RequestParam List<Long> eventIds,
            @RequestParam ReactionType reaction,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @PositiveOrZero Integer size) {
        return eventReactionService.getUsersByReaction(eventIds, reaction, from, size);
    }

    @GetMapping("/by-users")
    public List<UserRatingStatsDto> getReactionsByUsersIds(@RequestParam List<Long> usersIds) {
        return eventReactionService.getUsersRatingStats(usersIds);
    }

}
