package ru.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.HitRequestDto;
import ru.practicum.dto.StatsViewDto;
import ru.practicum.model.Hit;
import ru.practicum.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public Hit saveHit(@RequestBody @Valid HitRequestDto hitRequestDto) {
        return statsService.saveHit(hitRequestDto);
    }

    @GetMapping("/stats")
    public List<StatsViewDto> getStats(@RequestParam(required = false) LocalDateTime start,
                                       @RequestParam(required = false) LocalDateTime end,
                                       @RequestParam(required = false) List<String> uris,
                                       @RequestParam(defaultValue = "false") Boolean unique) {
        return statsService.getStats(start, end, uris, unique);
    }
}