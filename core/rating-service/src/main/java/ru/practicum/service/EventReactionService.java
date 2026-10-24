package ru.practicum.service;

import ru.practicum.dto.*;
import ru.practicum.model.ReactionType;

import java.util.List;
import java.util.Map;

public interface EventReactionService {

    EventReactionDto addReaction(Long userId, Long eventId, ReactionType reactionType);

    void deleteReaction(Long userId, Long eventId, ReactionType reactionType);

    List<UserShortDto> getUsersByReaction(List<Long> eventIds, ReactionType reactionType, Integer from, Integer size);

    Map<Long, Integer> getRatings(List<Long> eventIds);

    List<EventFullDto> getFavoriteEvents(Long userId);

    List<EventShortDto> getTopEventsByRating(Integer limit, String order);

    List<UserRatingStatsDto> getUsersRatingStats(List<Long> userIds);
}