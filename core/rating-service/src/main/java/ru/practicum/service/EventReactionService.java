package ru.practicum.service;

import ru.practicum.dto.EventReactionDto;
import ru.practicum.dto.UserRatingStatsDto;
import ru.practicum.dto.UserShortDto;
import ru.practicum.model.ReactionType;

import java.util.List;
import java.util.Map;

public interface EventReactionService {

    EventReactionDto addReaction(Long userId, Long eventId, ReactionType reactionType);

    void deleteReaction(Long userId, Long eventId, ReactionType reactionType);

    List<UserRatingStatsDto> getUsersRatingStats(List<Long> userIds);

    List<UserShortDto> getUsersByReaction(List<Long> eventIds, ReactionType reactionType, Integer from, Integer size);

    Map<Long, Integer> getRatings(List<Long> eventIds);
}