package ru.practicum.service.compilation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.compilation.CompilationRequest;
import ru.practicum.dto.compilation.CompilationResponse;
import ru.practicum.dto.compilation.GetCompilationListDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.exception.AlreadyExistsException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;
import ru.practicum.repository.CompilationRepository;
import ru.practicum.repository.event.EventRepository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final CompilationMapper compilationMapper;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CompilationResponse save(CompilationRequest compilationRequest) {
        Set<Long> eventIds = new HashSet<>(compilationRequest.getEvents());
        boolean exists = compilationRepository.existsByTitleAndSameEvents(compilationRequest.getTitle(),
                eventIds,
                eventIds.size());

        if (exists) {
            throw new AlreadyExistsException("Compilation with same title and events already exists");
        }

        Set<Event> events = new HashSet<>(eventRepository.findAllById(eventIds));
        Compilation compilation = compilationRepository.save(compilationMapper.toModel(compilationRequest));
        compilation.setEvents(events);
        return compilationMapper.toResponse(compilation);
    }

    @Override
    @Transactional
    public CompilationResponse update(UpdateCompilationRequest compilationRequest, Long compId) {
        Compilation compilation = compilationRepository.findById(compId).orElseThrow(
                () -> new NotFoundException(String.format("Compilation with id=%s was not found", compId)));

        if (compilationRequest.getPinned() != null && !(compilationRequest.getPinned()).equals(compilation.getPinned())) {
            compilation.setPinned(compilationRequest.getPinned());
        }
        if (compilationRequest.getTitle() != null && !(compilationRequest.getTitle()).equals(compilation.getTitle())) {
            compilation.setTitle(compilationRequest.getTitle());
        }

        if (compilationRequest.getEvents() != null) {
            Set<Long> requestEventIds = compilationRequest.getEvents();
            Set<Long> currentEventIds = compilation.getEvents().stream()
                    .map(Event::getId)
                    .collect(Collectors.toSet());

            if (!requestEventIds.equals(currentEventIds)) {
                List<Event> foundEvents = eventRepository.findAllById(requestEventIds);
                Set<Long> foundEventIds = foundEvents.stream()
                        .map(Event::getId)
                        .collect(Collectors.toSet());
                if (!foundEventIds.equals(requestEventIds)) {
                    throw new NotFoundException("Some events were not found");
                }
                compilation.setEvents(new HashSet<>(foundEvents));
            }

        }

        return compilationMapper.toResponse(compilationRepository.save(compilation));
    }

    @Override
    @Transactional
    public void delete(Long compId) {
        Compilation compilation = compilationRepository.findById(compId).orElseThrow(
                () -> new NotFoundException(String.format("Compilation with id=%s was not found", compId)));
        compilationRepository.delete(compilation);
    }

    @Override
    public CompilationResponse findById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId).orElseThrow(
                () -> new NotFoundException(String.format("Compilation with id=%s was not found", compId)));
        return compilationMapper.toResponse(compilation);
    }

    @Override
    public Collection<CompilationResponse> findNeededCompilation(GetCompilationListDto getCompilationListDto) {
        return compilationRepository.findNeededCompilations(getCompilationListDto.getPinned(),
                        getCompilationListDto.getFrom(),
                        getCompilationListDto.getSize())
                .stream()
                .map(compilationMapper::toResponse)
                .collect(Collectors.toList());
    }
}
