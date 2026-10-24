package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.EventRatingProjection;
import ru.practicum.model.EventReaction;
import ru.practicum.model.ReactionProjection;
import ru.practicum.model.ReactionType;

import java.util.List;
import java.util.Optional;

public interface EventReactionRepository extends JpaRepository<EventReaction, Long> {

    Optional<EventReaction> findByReactorIdAndEventId(Long userId, Long eventId);

    @Query("""
    SELECT r.eventId AS eventId,
           r.reactionType AS reaction
    FROM EventReaction r
    WHERE r.eventId IN :eventIds
    """)
    List<ReactionProjection> findEventReactionsByEventIds(@Param("eventIds") List<Long> eventIds);

    @Query("""
    SELECT DISTINCT r.reactorId
    FROM EventReaction r
    WHERE r.eventId IN :eventIds
    AND r.reactionType = :reactionType
    """)
    List<Long> findReactorIdsByEventIdsAndReactionType(
            @Param("eventIds") List<Long> eventIds,
            @Param("reactionType") ReactionType reactionType,
            Pageable pageable
    );

    @Query("""
    SELECT r.eventId
    FROM EventReaction r
    WHERE r.reactorId = :userId
      AND r.reactionType = :reactionType
    """)
    List<Long> findEventIdsByReactorIdAndReactionType(
            @Param("userId") Long userId,
            @Param("reactionType") ReactionType reactionType
    );

    @Query("""
    SELECT r.eventId AS eventId,
           SUM(
               CASE
                   WHEN r.reactionType = ru.practicum.model.ReactionType.LIKE THEN 1
                   WHEN r.reactionType = ru.practicum.model.ReactionType.DISLIKE THEN -1
                   ELSE 0
               END
           ) AS rating
    FROM EventReaction r
    GROUP BY r.eventId
    """)
    List<EventRatingProjection> findAllEventRatings();
}