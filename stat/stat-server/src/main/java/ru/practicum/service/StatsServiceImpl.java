package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.HitRequestDto;
import ru.practicum.dto.StatsViewDto;
import ru.practicum.dto.StatsViewProjection;
import ru.practicum.mapper.HitMapper;
import ru.practicum.model.Hit;
import ru.practicum.storage.HitRepository;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {
    private final HitRepository hitRepository;

    @Override
    public List<StatsViewDto> getStats(LocalDateTime start,
                                       LocalDateTime end,
                                       List<String> uris,
                                       Boolean unique) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Дата и время начала не может быть позже даты и времени окончания");
        }

        List<String> urisParam = (uris == null || uris.isEmpty()) ? null : uris;

        List<StatsViewProjection> statsViewProjections = Boolean.TRUE.equals(unique) ?
                hitRepository.findUniqueStatsViewProjections(start, end, urisParam) :
                hitRepository.findStatsViewProjections(start, end, urisParam);

        return statsViewProjections.stream()
                .map(projection -> new StatsViewDto(projection.getApp(), projection.getUri(), projection.getHits()))
                .toList();
    }

    @Override
    @Transactional
    public Hit saveHit(HitRequestDto hitRequestDto) {
        Hit hit = HitMapper.toHit(hitRequestDto);
        Hit savedHit = hitRepository.save(hit);

        log.info("Сохранено посещение с id = {}", savedHit.getId());
        return savedHit;
    }
}