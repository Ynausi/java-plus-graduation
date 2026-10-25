package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.model.EventSimilarity;
import java.util.List;
import java.util.Optional;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {
    Optional<EventSimilarity> findByEventAAndEventB(Long eventA, Long eventB);

    @Query("""
            select similarity from EventSimilarity similarity
            where similarity.eventA = :eventId or similarity.eventB = :eventId
            order by similarity.score desc
            """)
    List<EventSimilarity> findForEvent(@Param("eventId") Long eventId);

    @Query("""
            select similarity from EventSimilarity similarity
            where similarity.eventA in :eventIds or similarity.eventB in :eventIds
            """)
    List<EventSimilarity> findForEvents(@Param("eventIds") List<Long> eventIds);
}
