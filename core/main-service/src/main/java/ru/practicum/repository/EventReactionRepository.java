package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.dto.users.UserRatingStatsDto;
import ru.practicum.model.EventReaction;
import ru.practicum.model.ReactionProjection;
import ru.practicum.model.ReactionType;
import ru.practicum.model.User;

import java.util.List;
import java.util.Optional;

public interface EventReactionRepository extends JpaRepository<EventReaction, Long> {

    Optional<EventReaction> findByReactorIdAndEventId(Long userId, Long eventId);

    @Query("SELECT r.event.id AS eventId, r.reactionType AS reaction " +
            "FROM EventReaction r " +
            "WHERE r.event.id IN :eventIds")
    List<ReactionProjection> findEventReactionsByEventIds(@Param("eventIds") List<Long> eventIds);

    @Query("SELECT r.reactor FROM EventReaction r WHERE r.event.id IN :eventIds AND r.reactionType = :reactionType")
    List<User> findReactorsByEventIdAndReactionType(@Param("eventIds") List<Long> eventIds,
                                                    @Param("reactionType") ReactionType reactionType,
                                                    Pageable pageable);

    @Query("SELECT new ru.practicum.dto.users.UserRatingStatsDto(" +
            "r.event.initiator.id, " +
            "SUM(CASE WHEN r.reactionType = 'LIKE' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN r.reactionType = 'DISLIKE' THEN 1 ELSE 0 END)) " +
            "FROM EventReaction r " +
            "WHERE r.event.initiator.id IN :userIds " +
            "GROUP BY r.event.initiator.id")
    List<UserRatingStatsDto> getStatsByUserIds(@Param("userIds") List<Long> userIds);
}