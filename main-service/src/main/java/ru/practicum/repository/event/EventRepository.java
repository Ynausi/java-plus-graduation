package ru.practicum.repository.event;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, EventQuerydslRepository {
    @Query("SELECT MIN(e.createdOn) FROM Event e WHERE e.id IN :eventIds")
    Optional<LocalDateTime> findEarliestCreatedOnByEventIds(@Param("eventIds") List<Long> eventIds);

    List<Event> findAllByInitiatorId(Long userId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    Boolean existsByCategoryId(Long categoryId);

    boolean existsById(Long id);

    @Query("SELECT e FROM Event e " +
            "JOIN e.eventReactions r " +
            "WHERE r.reactor.id = :userId " +
            "AND r.reactionType = 'LIKE'")
    List<Event> findFavoriteEvents(@Param("userId") Long userId);

    @Query("SELECT e " +
            "FROM Event e " +
            "LEFT JOIN e.eventReactions r " +
            "GROUP BY e.id " +
            "ORDER BY (SUM(CASE WHEN r.reactionType = 'LIKE' THEN 1L ELSE 0L END) - " +
            "         SUM(CASE WHEN r.reactionType = 'DISLIKE' THEN 1L ELSE 0L END)) DESC " +
            "LIMIT :limit")
    List<Event> findTopEventsByRatingDesc(@Param("limit") Integer limit);

    @Query("SELECT e " +
            "FROM Event e " +
            "LEFT JOIN e.eventReactions r " +
            "GROUP BY e.id " +
            "ORDER BY (SUM(CASE WHEN r.reactionType = 'LIKE' THEN 1L ELSE 0L END) - " +
            "         SUM(CASE WHEN r.reactionType = 'DISLIKE' THEN 1L ELSE 0L END)) ASC " +
            "LIMIT :limit")
    List<Event> findTopEventsByRatingAsc(@Param("limit") Integer limit);
}