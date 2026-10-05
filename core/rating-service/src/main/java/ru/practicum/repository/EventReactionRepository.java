package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

}