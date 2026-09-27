package ru.practicum.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.dto.StatsViewProjection;
import ru.practicum.model.Hit;

import java.time.LocalDateTime;
import java.util.List;

public interface HitRepository extends JpaRepository<Hit, Long> {

    @Query("""
            SELECT h.app as app, h.uri as uri, COUNT(h.ip) as hits FROM Hit h
            WHERE h.timestamp BETWEEN :start AND :end
            AND (:uris IS NULL OR uri IN :uris)
            GROUP BY h.app, h.uri
            ORDER BY COUNT(h.ip) DESC
            """)
    List<StatsViewProjection> findStatsViewProjections(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("uris") List<String> uris);

    @Query("""
            SELECT h.app as app, h.uri as uri, COUNT(DISTINCT h.ip) as hits FROM Hit h
            WHERE h.timestamp BETWEEN :start AND :end
            AND (:uris IS NULL OR uri IN :uris)
            GROUP BY h.app, h.uri
            ORDER BY COUNT(DISTINCT h.ip) DESC
            """)
    List<StatsViewProjection> findUniqueStatsViewProjections(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("uris") List<String> uris);
}
