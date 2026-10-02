package ru.practicum.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatsClient;
import ru.practicum.dto.*;
import ru.practicum.exceptions.BadRequestException;
import ru.practicum.exceptions.ConflictException;
import ru.practicum.exceptions.NotFoundException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.Event;
import ru.practicum.model.EventState;
import ru.practicum.model.ReactionProjection;
import ru.practicum.repository.EventReactionRepository;
import ru.practicum.repository.EventRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.practicum.model.EventState.PUBLISHED;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventReactionRepository eventReactionRepository;
    private final StatsClient statsClient;
    private final EventMapper eventMapper;
    private final EventRepository eventRepository;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final int MIN_HOURS_BEFORE_EVENT = 2;
    private static final int DEFAULT_STATS_RANGE_MONTHS = 6;

    @Override
    public List<EventShortDto> getEventsByUser(Long userId, Integer from, Integer size) {
        getUserByIdOrThrow(userId);
        int page = from / size;
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());

        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);

        return enrichShortDtos(events);
    }

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        User initiator = getUserByIdOrThrow(userId);

        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT))) {
            throw new BadRequestException(
                    String.format("Event date must be at least %d hours from now", MIN_HOURS_BEFORE_EVENT)
            );
        }

        Category category = getCategoryByIdOrThrow(newEventDto.getCategory());
        Event newEvent = eventMapper.toEvent(newEventDto);

        newEvent.setInitiator(initiator);
        newEvent.setCategory(category);

        if (newEvent.getPaid() == null) newEvent.setPaid(false);
        if (newEvent.getParticipantLimit() == null) newEvent.setParticipantLimit(0);
        if (newEvent.getRequestModeration() == null) newEvent.setRequestModeration(true);

        newEvent.setEventState(EventState.PENDING);

        Event createdEvent = eventRepository.save(newEvent);

        EventFullDto fullDto = eventMapper.toEventFullDto(createdEvent);
        fullDto.setViews(0L);
        fullDto.setConfirmedRequests(0L);
        return fullDto;
    }

    @Override
    public EventFullDto getEventById(Long userId, Long eventId) {
        getUserByIdOrThrow(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id - " + eventId + " не найдено"));

        Map<Long, Long> viewsMap = getViewsMap(List.of(eventId));
        Map<Long, Integer> ratingsMap = getRatingsMap(List.of(eventId));

        Long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        EventFullDto eventFullDto = eventMapper.toEventFullDto(event);
        eventFullDto.setViews(viewsMap.getOrDefault(eventId, 0L));
        eventFullDto.setConfirmedRequests(confirmedRequests);
        eventFullDto.setRating(ratingsMap.getOrDefault(eventId, 0));
        return eventFullDto;
    }

    @Override
    public EventForRequestDto getEventById(Long eventId) {
        return null;
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest) {
        getUserByIdOrThrow(userId);
        Event event = getEventByIdOrThrow(eventId);

        if (!event.getInitiator().equals(userId)) {
            throw new ConflictException("Пользователь не является инициатором этого события");
        }

        if (updateEventUserRequest.getEventDate() != null) {
            if (updateEventUserRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT))) {
                throw new BadRequestException(
                        String.format("Event date must be at least %d hours from now", MIN_HOURS_BEFORE_EVENT)
                );
            }
            event.setEventDate(updateEventUserRequest.getEventDate());
        }

        if (event.getEventState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Событие уже опубликовано");
        }

        if (updateEventUserRequest.getStateAction() != null) {
            if (updateEventUserRequest.getStateAction().equals(UserStateAction.SEND_TO_REVIEW)) {
                event.setEventState(EventState.PENDING);
            } else if (updateEventUserRequest.getStateAction().equals(UserStateAction.CANCEL_REVIEW)) {
                event.setEventState(EventState.CANCELED);
            }
        }

        if (updateEventUserRequest.getAnnotation() != null)
            event.setAnnotation(updateEventUserRequest.getAnnotation());

        if (updateEventUserRequest.getCategory() != null)
            event.setCategory(getCategoryByIdOrThrow(updateEventUserRequest.getCategory()));

        if (updateEventUserRequest.getDescription() != null)
            event.setDescription(updateEventUserRequest.getDescription());

        if (updateEventUserRequest.getLocation() != null)
            event.setLocation(eventMapper.toLocation(updateEventUserRequest.getLocation()));

        if (updateEventUserRequest.getPaid() != null)
            event.setPaid(updateEventUserRequest.getPaid());

        if (updateEventUserRequest.getParticipantLimit() != null)
            event.setParticipantLimit(updateEventUserRequest.getParticipantLimit());

        if (updateEventUserRequest.getRequestModeration() != null)
            event.setRequestModeration(updateEventUserRequest.getRequestModeration());

        if (updateEventUserRequest.getTitle() != null)
            event.setTitle(updateEventUserRequest.getTitle());

        return eventMapper.toEventFullDto(eventRepository.save(event));
    }

    @Override
    public List<EventFullDto> getEventsByAdmin(EventSearchFilterAdmin filter,
                                               Integer from,
                                               Integer size) {
        int page = from / size;
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());

        List<Event> events = eventRepository.searchAdmin(filter, pageable);

        return enrichFullDtos(events);
    }

    @Transactional
    @Override
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = getEventByIdOrThrow(eventId);

        if (request.getEventDate() != null) {
            if (request.getEventDate().isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT))) {
                throw new BadRequestException(
                        String.format("Event date must be at least %d hours from now", MIN_HOURS_BEFORE_EVENT)
                );
            }
            event.setEventDate(request.getEventDate());
        }

        if (request.getStateAction() != null) {
            if (request.getStateAction().equals(AdminStateAction.PUBLISH_EVENT)) {
                if (!event.getEventState().equals(EventState.PENDING)) {
                    throw new ConflictException("Можно публиковать только события в состоянии ожидания публикации");
                }
                event.setEventState(PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (request.getStateAction().equals(AdminStateAction.REJECT_EVENT)) {
                if (event.getEventState().equals(PUBLISHED)) {
                    throw new ConflictException("Нельзя отклонить уже опубликованное событие");
                }
                event.setEventState(EventState.CANCELED);
            }
        }

        if (request.getAnnotation() != null)
            event.setAnnotation(request.getAnnotation());

        if (request.getCategory() != null)
            event.setCategory(getCategoryByIdOrThrow(request.getCategory()));

        if (request.getDescription() != null)
            event.setDescription(request.getDescription());

        if (request.getLocation() != null)
            event.setLocation(eventMapper.toLocation(request.getLocation()));

        if (request.getPaid() != null)
            event.setPaid(request.getPaid());

        if (request.getParticipantLimit() != null)
            event.setParticipantLimit(request.getParticipantLimit());

        if (request.getRequestModeration() != null)
            event.setRequestModeration(request.getRequestModeration());

        if (request.getTitle() != null)
            event.setTitle(request.getTitle());

        log.info("Событие с id - {} обновлено администратором", eventId);

        return eventMapper.toEventFullDto(eventRepository.save(event));
    }

    @Override
    @Transactional
    public List<EventShortDto> getEventsPublic(EventSearchFilterPublic filter,
                                               Integer from,
                                               Integer size,
                                               HttpServletRequest request) {

        statsClient.hit(new HitRequestDto("ewm-main-ru.practicum.service", request.getRequestURI(), request.getRemoteAddr(),
                LocalDateTime.now()));

        int page = from / size;
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());

        List<Event> events = eventRepository.searchPublic(filter, pageable);

        return enrichShortDtos(events);
    }

    @Override
    @Transactional
    public EventFullDto getPublicEventById(Long eventId, HttpServletRequest request) {
        Event event = getEventByIdOrThrow(eventId);

        if (!event.getEventState().equals(PUBLISHED)) {
            throw new NotFoundException("Событие с id - " + eventId + " не найдено");
        }

        statsClient.hit(new HitRequestDto("ewm-main-ru.practicum.service", request.getRequestURI(), request.getRemoteAddr(),
                LocalDateTime.now()));

        Map<Long, Long> viewsMap = getViewsMap(List.of(eventId));
        Long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

        EventFullDto dto = eventMapper.toEventFullDto(event);
        dto.setViews(viewsMap.getOrDefault(eventId, 0L));
        dto.setConfirmedRequests(confirmedRequests);
        return dto;
    }

    @Override
    public List<EventFullDto> getFavoriteEvents(Long userId) {
        getUserByIdOrThrow(userId);

        List<Event> favoriteEvents = eventRepository.findFavoriteEvents(userId);

        return enrichFullDtos(favoriteEvents);
    }

    @Override
    public List<EventShortDto> getTopEventsByRating(Integer limit, String order) {
        List<Event> events;

        if (order != null && order.equalsIgnoreCase("ASC")) {
            events = eventRepository.findTopEventsByRatingAsc(limit);
        } else {
            events = eventRepository.findTopEventsByRatingDesc(limit);
        }

        return enrichShortDtos(events);
    }

    private Map<Long, Long> getViewsMap(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) return Collections.emptyMap();

        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .toList();

        LocalDateTime earliestDateTime = eventRepository.findEarliestCreatedOnByEventIds(eventIds)
                .orElse(LocalDateTime.now().minusMonths(DEFAULT_STATS_RANGE_MONTHS));

        String start = earliestDateTime.format(FORMATTER);
        String end = LocalDateTime.now().format(FORMATTER);


        try {
            List<StatsViewDto> stats = statsClient.getStats(start, end, uris, true);

            if (stats == null || stats.isEmpty()) return Collections.emptyMap();

            return stats.stream()
                    .collect(Collectors.toMap(
                            statsDto -> {
                                String uri = statsDto.getUri();
                                return Long.parseLong(uri.substring(uri.lastIndexOf("/") + 1));
                            },
                            StatsViewDto::getHits,
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            log.warn("Stats ru.practicum.service is not available, returning empty views map");
            return Collections.emptyMap();
        }
    }

    private Map<Long, Long> getConfirmedRequestsMap(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) return Collections.emptyMap();

        List<Object[]> result = requestRepository.countByEventIdsAndStatus(eventIds, RequestStatus.CONFIRMED);

        return result.stream()
                .collect(Collectors.toMap(
                        objects -> (Long) objects[0],
                        objects -> (Long) objects[1],
                        (existing, replacement) -> existing
                ));
    }

    private Map<Long, Integer> getRatingsMap(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) return Collections.emptyMap();

        List<ReactionProjection> eventReactions = eventReactionRepository.findEventReactionsByEventIds(eventIds);

        return eventReactions.stream()
                .collect(Collectors.groupingBy(
                        ReactionProjection::getEventId,
                        Collectors.summingInt(projection -> projection.getReaction().getWeight())
                ));
    }

    private List<EventFullDto> enrichFullDtos(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyList();

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> viewsMap = getViewsMap(eventIds);
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(eventIds);
        Map<Long, Integer> ratingsMap = getRatingsMap(eventIds);

        return events.stream()
                .map(event -> {
                    EventFullDto dto = eventMapper.toEventFullDto(event);
                    dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                    dto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0L));
                    dto.setRating(ratingsMap.getOrDefault(event.getId(), 0));
                    return dto;
                })
                .toList();
    }

    private List<EventShortDto> enrichShortDtos(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyList();

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> viewsMap = getViewsMap(eventIds);
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(eventIds);
        Map<Long, Integer> ratingsMap = getRatingsMap(eventIds);

        return events.stream()
                .map(event -> {
                    EventShortDto shortDto = eventMapper.toEventShortDto(event);
                    shortDto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                    shortDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0L));
                    shortDto.setRating(ratingsMap.getOrDefault(event.getId(), 0));
                    return shortDto;
                })
                .toList();
    }

    private Event getEventByIdOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(
                        () -> new NotFoundException("Событие с id - " + eventId + " не найдено")
                );
    }
}