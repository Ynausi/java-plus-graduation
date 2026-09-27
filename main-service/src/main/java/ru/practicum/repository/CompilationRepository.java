package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Compilation;

import java.util.Collection;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    @Query(value = """
            select exists (
                select 1
                from compilations c
                left join compilations_events ce on c.id = ce.compilation_id
                where c.title = :title
                group by c.id
                having count(distinct ce.event_id) = :eventsSize
                   and count(distinct case
                        when ce.event_id in (:eventIds) then ce.event_id
                   end) = :eventsSize
            )
            """, nativeQuery = true)
    boolean existsByTitleAndSameEvents(@Param("title") String title,
                                       @Param("eventIds") Collection<Long> eventIds,
                                       @Param("eventsSize") long eventsSize);

    @Query(value = """
            select *
            from compilations c
            where c.pinned = :pinned
            order by c.id asc
            limit :size offset :from
            """,
            nativeQuery = true)
    Collection<Compilation> findNeededCompilations(@Param("pinned") Boolean pinned,
                                                   @Param("from") Integer from,
                                                   @Param("size") Integer size);

}
