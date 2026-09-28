package ru.practicum.controller.compilations;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.compilation.CompilationResponse;
import ru.practicum.dto.compilation.GetCompilationListDto;
import ru.practicum.service.compilation.CompilationService;

import java.util.Collection;

@RestController
@RequestMapping("/compilations")
@RequiredArgsConstructor
public class PublicCompilationController {
    private final CompilationService compilationService;

    @GetMapping("/{compId}")
    public ResponseEntity<CompilationResponse> getById(@PathVariable("compId") Long compId) {
        return ResponseEntity.ok().body(compilationService.findById(compId));
    }

    @GetMapping
    public ResponseEntity<Collection<CompilationResponse>> getNeededCompilation(
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size,
            @RequestParam(defaultValue = "false") Boolean pinned) {
        return ResponseEntity.ok()
                .body(compilationService.findNeededCompilation(new GetCompilationListDto(size, pinned, from)));
    }
}
