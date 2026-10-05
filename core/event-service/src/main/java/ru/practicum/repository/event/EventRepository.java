package ru.practicum.repository.event;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Event;
import ru.practicum.repository.event.EventQuerydslRepository;

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
}