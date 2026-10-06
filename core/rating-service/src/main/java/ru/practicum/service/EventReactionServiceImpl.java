package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.EventClient;
import ru.practicum.client.RequestClient;
import ru.practicum.client.UserClient;
import ru.practicum.dto.*;
import ru.practicum.exceptions.BadRequestException;
import ru.practicum.exceptions.ConflictException;
import ru.practicum.exceptions.NotFoundException;
import ru.practicum.mapper.ReactionMapper;
import ru.practicum.model.EventRatingProjection;
import ru.practicum.model.EventReaction;
import ru.practicum.model.ReactionProjection;
import ru.practicum.model.ReactionType;
import ru.practicum.repository.EventReactionRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventReactionServiceImpl implements EventReactionService {

    private final EventReactionRepository reactionRepository;
    private final UserClient userClient;
    private final RequestClient requestClient;
    private final EventClient eventClient;
    private final ReactionMapper reactionMapper;

    @Override
    public List<UserShortDto> getUsersByReaction(List<Long> eventIds, ReactionType reactionType, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);

        List<Long> reactorsIds = reactionRepository.findReactorIdsByEventIdsAndReactionType(eventIds, reactionType, pageable);

        if (reactorsIds.isEmpty()) {
            return Collections.emptyList();
        }

        return userClient.getUsers(reactorsIds)
                .stream()
                .map(user -> new UserShortDto(
                        user.getId(),
                        user.getName()
                ))
                .toList();
    }

    @Override
    @Transactional
    public EventReactionDto addReaction(Long userId, Long eventId, ReactionType reactionType) {
        userClient.getUser(userId);
        eventClient.getEvent(eventId);

        Optional<EventReaction> existingReaction = reactionRepository.findByReactorIdAndEventId(userId, eventId);

        validateUserParticipant(userId, eventId);

        EventReaction reaction;
        if (existingReaction.isPresent()) {
            reaction = existingReaction.get();

            if (reaction.getReactionType().equals(reactionType)) {
                throw new ConflictException("Already reacted this event");
            }

            reaction.setReactionType(reactionType);
            reaction.setUpdatedAt(LocalDateTime.now());

        } else {
            EventReaction newReaction = reactionMapper.toEntity(userId, eventId, reactionType);
            reaction = reactionRepository.save(newReaction);
        }


        return reactionMapper.toDto(reaction);
    }

    @Override
    public Map<Long, Integer> getRatings(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return reactionRepository.findEventReactionsByEventIds(eventIds)
                .stream()
                .collect(Collectors.groupingBy(
                        ReactionProjection::getEventId,
                        Collectors.summingInt(
                                p -> p.getReaction().getWeight()
                        )
                ));
    }

    @Override
    @Transactional
    public void deleteReaction(Long userId, Long eventId, ReactionType reactionType) {
        userClient.getUser(userId);
        eventClient.getEvent(eventId);

        EventReaction reaction = reactionRepository.findByReactorIdAndEventId(userId, eventId)
                .filter(r -> r.getReactionType().equals(reactionType))
                .orElseThrow(() -> new NotFoundException(
                        String.format("Reaction %s not found for this event", reactionType)
                ));

        reactionRepository.delete(reaction);
    }

    @Override
    public List<EventFullDto> getFavoriteEvents(Long userId) {

        userClient.getUser(userId);
        List<Long> eventIds = reactionRepository.findEventIdsByReactorIdAndReactionType(userId, ReactionType.LIKE);

        if (eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<EventFullDto> events = eventClient.getFullEventsByIds(eventIds);
        Map<Long, Integer> ratings = getRatings(eventIds);
        events.forEach(event -> event.setRating(ratings.getOrDefault(event.getId(), 0)));

        return events;
    }

    @Override
    public List<EventShortDto> getTopEventsByRating(Integer limit, String order) {

        List<EventRatingProjection> ratings = reactionRepository.findAllEventRatings();

        Comparator<EventRatingProjection> comparator = Comparator.comparingLong(EventRatingProjection::getRating);

        if (!"ASC".equalsIgnoreCase(order)) {
            comparator = comparator.reversed();
        }

        List<Long> eventIds = ratings.stream()
                .sorted(comparator)
                .limit(limit)
                .map(EventRatingProjection::getEventId)
                .toList();

        if (eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<EventShortDto> events = eventClient.getShortEventsByIds(eventIds);

        Map<Long, Integer> ratingsMap =
                ratings.stream()
                        .collect(Collectors.toMap(EventRatingProjection::getEventId,
                                rating -> Math.toIntExact(rating.getRating())));

        events.forEach(event -> event.setRating(ratingsMap.getOrDefault(event.getId(), 0)));

        return events;
    }

    @Override
    public List<UserRatingStatsDto> getUsersRatingStats(List<Long> userIds) {

        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> eventOwners = eventClient.getEventOwnersByInitiatorIds(userIds);

        if (eventOwners.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> eventIds = eventOwners.keySet()
                .stream()
                .toList();

        List<ReactionProjection> reactions = reactionRepository.findEventReactionsByEventIds(eventIds);
        Map<Long, UserRatingStatsDto> stats = new HashMap<>();

        for (ReactionProjection reaction : reactions) {

            Long ownerId = eventOwners.get(reaction.getEventId());
            if (ownerId == null) {
                continue;
            }

            UserRatingStatsDto userStats = stats.computeIfAbsent(ownerId,
                    id -> new UserRatingStatsDto(id, 0L, 0L));

            if (reaction.getReaction() == ReactionType.LIKE) {
                userStats.setLikes(userStats.getLikes() + 1);
            } else if (reaction.getReaction() == ReactionType.DISLIKE) {
                userStats.setDislikes(userStats.getDislikes() + 1);
            }
        }

        return userIds.stream()
                .distinct()
                .map(stats::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private void validateUserParticipant(Long userId, Long eventId) {
        boolean isParticipant = requestClient.isConfirmedParticipant(userId,eventId);

        if (!isParticipant) {
            throw new BadRequestException("Only participants can react to events");
        }
    }

}