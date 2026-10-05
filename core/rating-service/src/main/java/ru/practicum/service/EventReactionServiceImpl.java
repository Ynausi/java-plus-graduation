package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.EventClient;
import ru.practicum.client.RequestClient;
import ru.practicum.client.UserClient;
import ru.practicum.dto.EventReactionDto;
import ru.practicum.dto.UserShortDto;
import ru.practicum.exceptions.BadRequestException;
import ru.practicum.exceptions.ConflictException;
import ru.practicum.exceptions.NotFoundException;
import ru.practicum.mapper.ReactionMapper;
import ru.practicum.model.EventReaction;
import ru.practicum.model.ReactionProjection;
import ru.practicum.model.ReactionType;
import ru.practicum.repository.EventReactionRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public EventReactionDto addReaction(Long userId,
                                              Long eventId,
                                              ReactionType reactionType) {
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

    private void validateUserParticipant(Long userId, Long eventId) {
        boolean isParticipant = requestClient.isConfirmedParticipant(userId,eventId);

        if (!isParticipant) {
            throw new BadRequestException("Only participants can react to events");
        }
    }

}