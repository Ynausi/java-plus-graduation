package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.model.UserInteraction;
import java.util.List;
import java.util.Optional;

public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {
    Optional<UserInteraction> findByUserIdAndEventId(Long userId, Long eventId);

    List<UserInteraction> findByUserId(Long userId);

    List<UserInteraction> findByUserIdOrderByUpdatedAtDesc(Long userId);

    @Query("""
            select interaction.eventId, sum(interaction.weight)
            from UserInteraction interaction
            where interaction.eventId in :eventIds
            group by interaction.eventId
            """)
    List<Object[]> sumWeightsByEventIds(@Param("eventIds") List<Long> eventIds);
}
