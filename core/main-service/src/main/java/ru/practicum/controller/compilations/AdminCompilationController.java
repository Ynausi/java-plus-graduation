package ru.practicum.controller.compilations;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.compilation.CompilationRequest;
import ru.practicum.dto.compilation.CompilationResponse;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.service.compilation.CompilationService;

import java.net.URI;

@RestController
@RequestMapping("/admin/compilations")
@RequiredArgsConstructor
@Slf4j
public class AdminCompilationController {
    private final CompilationService compilationService;

    @PostMapping
    public ResponseEntity<CompilationResponse> save(@Valid @RequestBody CompilationRequest compilationRequest) {
        log.info("Create compilation");
        CompilationResponse created = compilationService.save(compilationRequest);
        return ResponseEntity.created(URI.create("/" + created.getId()))
                .body(created);
    }

    @PatchMapping("/{compId}")
    public ResponseEntity<CompilationResponse> update(@Valid @RequestBody UpdateCompilationRequest compilationRequest,
                                                      @PathVariable("compId") Long compId) {
        log.info("Update compilation with id: {}", compId);
        return ResponseEntity.ok().body(compilationService.update(compilationRequest, compId));
    }

    @DeleteMapping("/{compId}")
    public ResponseEntity<Void> delete(@PathVariable("compId") Long compId) {
        log.info("Delete compilation with id: {}", compId);
        compilationService.delete(compId);
        return ResponseEntity.noContent().build();
    }
}
