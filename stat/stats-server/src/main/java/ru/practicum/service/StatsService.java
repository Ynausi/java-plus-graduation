package ru.practicum.service;

import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import ru.practicum.dto.HitRequestDto;
import ru.practicum.dto.StatsViewDto;
import ru.practicum.model.Hit;

import java.time.LocalDateTime;
import java.util.List;

@Validated
public interface StatsService {

    List<StatsViewDto> getStats(@NotNull(message = "дата начала фильтра должна быть задана")
                                LocalDateTime start,
                                @NotNull(message = "дата окончания фильтра должна быть задана")
                                LocalDateTime end,
                                List<String> uris,
                                Boolean unique);

    Hit saveHit(HitRequestDto hitRequestDto);
}