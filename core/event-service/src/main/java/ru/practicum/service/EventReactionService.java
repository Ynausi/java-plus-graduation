package ru.practicum.service.event;

import ru.practicum.dto.event.EventReactionDto;
import ru.practicum.dto.users.UserRatingStatsDto;
import ru.practicum.dto.users.UserShortDto;
import ru.practicum.model.ReactionType;

import java.util.List;

public interface EventReactionService {

    EventReactionDto addReaction(Long userId, Long eventId, ReactionType reactionType);

    void deleteReaction(Long userId, Long eventId, ReactionType reactionType);

    List<UserRatingStatsDto> getUsersRatingStats(List<Long> userIds);

    List<UserShortDto> getUsersByReaction(List<Long> eventIds, ReactionType reactionType, Integer from, Integer size);
}