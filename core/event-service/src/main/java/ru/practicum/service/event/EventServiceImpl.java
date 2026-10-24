package ru.practicum.service.event;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.RequestClient;
import ru.practicum.client.UserClient;
import ru.practicum.dto.*;
import ru.practicum.exceptions.BadRequestException;
import ru.practicum.exceptions.ConflictException;
import ru.practicum.exceptions.NotFoundException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.Category;
import ru.practicum.model.Event;
import ru.practicum.model.EventState;
import ru.practicum.repository.category.CategoryRepository;
import ru.practicum.repository.event.EventRepository;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.stats.AnalyzerClient;
import ru.practicum.stats.CollectorClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static ru.practicum.model.EventState.PUBLISHED;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventMapper eventMapper;
    private final EventRepository eventRepository;
    private final UserClient userClient;
    private final RequestClient requestClient;
    private final CategoryRepository categoryRepository;
    private final CollectorClient collectorClient;
    private final AnalyzerClient analyzerClient;

    private static final int MIN_HOURS_BEFORE_EVENT = 2;

    @Override
    public List<EventShortDto> getEventsByUser(Long userId, Integer from, Integer size) {
        userClient.getUser(userId);
        int page = from / size;
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());

        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);

        return enrichShortDtos(events);
    }

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        UserDto initiator = userClient.getUser(userId);

        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT))) {
            throw new BadRequestException(
                    String.format("Event date must be at least %d hours from now", MIN_HOURS_BEFORE_EVENT)
            );
        }

        Category category = getCategoryByIdOrThrow(newEventDto.getCategory());
        Event newEvent = eventMapper.toEvent(newEventDto);

        newEvent.setInitiatorId(initiator.getId());
        newEvent.setCategory(category);

        if (newEvent.getPaid() == null) newEvent.setPaid(false);
        if (newEvent.getParticipantLimit() == null) newEvent.setParticipantLimit(0);
        if (newEvent.getRequestModeration() == null) newEvent.setRequestModeration(true);

        newEvent.setEventState(EventState.PENDING);

        Event createdEvent = eventRepository.save(newEvent);

        EventFullDto fullDto = eventMapper.toEventFullDto(createdEvent);

        fullDto.setConfirmedRequests(0L);
        fullDto.setRating(0.0);

        fullDto.setInitiator(new UserShortDto(initiator.getId(), initiator.getName()));
        return fullDto;
    }

    @Override
    public EventFullDto getEventById(Long userId, Long eventId) {
        userClient.getUser(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id - " + eventId + " не найдено"));


        return enrichFullDtos(List.of(event)).get(0);
    }

    @Override
    public EventForRequestDto getEventById(Long eventId) {

        Event event = eventRepository.findById(eventId).orElseThrow(() ->
                new NotFoundException("No event with id:" + eventId));
        return eventMapper.toEventForRequestDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest) {
        userClient.getUser(userId);
        Event event = getEventByIdOrThrow(eventId);

        if (!event.getInitiatorId().equals(userId)) {
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


        Event savedEvent =
                eventRepository.save(event);

        return enrichFullDtos(
                List.of(savedEvent)
        ).get(0);
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

        log.info(
                "Событие с id - {} обновлено администратором",
                eventId
        );

        Event savedEvent =
                eventRepository.save(event);

        return enrichFullDtos(
                List.of(savedEvent)
        ).get(0);
    }

    @Override
    @Transactional
    public List<EventShortDto> getEventsPublic(EventSearchFilterPublic filter,
                                               Integer from,
                                               Integer size,
                                               HttpServletRequest request) {

        int page = from / size;
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());

        List<Event> events = eventRepository.searchPublic(filter, pageable);

        return enrichShortDtos(events);
    }

    @Override
    @Transactional
    public EventFullDto getPublicEventById(Long eventId, Long userId) {
        Event event = getEventByIdOrThrow(eventId);

        if (!event.getEventState().equals(PUBLISHED)) {
            throw new NotFoundException("Событие с id - " + eventId + " не найдено");
        }

        userClient.getUser(userId);
        collectorClient.collectUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW);

        Long confirmedRequests = requestClient.getConfirmedCount(eventId);
        Map<Long, Double> ratingsMap = getRecommendationRatingsMap(List.of(eventId));
        UserDto user = userClient.getUser(event.getInitiatorId());

        EventFullDto dto = eventMapper.toEventFullDto(event);
        dto.setConfirmedRequests(confirmedRequests);
        dto.setRating(ratingsMap.getOrDefault(eventId, 0.0));
        dto.setInitiator(new UserShortDto(user.getId(), user.getName()));
        return dto;
    }

    @Override
    public List<EventShortDto> getRecommendations(Long userId, Integer maxResults) {
        userClient.getUser(userId);

        List<RecommendedEventProto> recommendations =
                analyzerClient.getRecommendationsForUser(userId, maxResults);

        if (recommendations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> eventIds = recommendations.stream()
                .map(RecommendedEventProto::getEventId)
                .toList();

        Map<Long, Event> eventsById = eventRepository.findAllById(eventIds)
                .stream()
                .filter(event -> event.getEventState().equals(PUBLISHED))
                .collect(Collectors.toMap(Event::getId, event -> event));

        List<Event> events = eventIds.stream()
                .map(eventsById::get)
                .filter(Objects::nonNull)
                .toList();

        return enrichShortDtos(events);
    }

    @Override
    public void likeEvent(Long userId, Long eventId) {
        userClient.getUser(userId);
        Event event = getEventByIdOrThrow(eventId);

        if (!event.getEventState().equals(PUBLISHED)) {
            throw new NotFoundException("Событие с id - " + eventId + " не найдено");
        }

        if (!requestClient.isConfirmedParticipant(userId, eventId)) {
            throw new BadRequestException(
                    "Only confirmed event participants can like an event"
            );
        }

        collectorClient.collectUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }

    @Override
    public List<EventFullDto> getFullEventsByIds(List<Long> eventIds) {

        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Event> eventsById = eventRepository.findAllById(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        Event::getId,
                        event -> event
                ));

        List<Event> events = eventIds.stream()
                .map(eventsById::get)
                .filter(Objects::nonNull)
                .toList();

        return enrichFullDtosWithoutRating(events);
    }

    @Override
    public List<EventShortDto> getShortEventsByIds(List<Long> eventIds) {

        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Event> eventsById = eventRepository.findAllById(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        Event::getId,
                        event -> event
                ));

        List<Event> events = eventIds.stream()
                .map(eventsById::get)
                .filter(Objects::nonNull)
                .toList();

        return enrichShortDtosWithoutRating(events);
    }

    @Override
    public Map<Long, Long> getEventOwnersByInitiatorIds(List<Long> userIds) {

        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return eventRepository.findAllByInitiatorIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(
                        Event::getId,
                        Event::getInitiatorId
                ));
    }

    private Map<Long, Long> getConfirmedRequestsMap(List<Long> eventIds) {

        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return requestClient.getConfirmedCounts(eventIds);
    }


    private List<EventFullDto> enrichFullDtos(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyList();

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(eventIds);
        Map<Long, Double> ratingsMap = getRecommendationRatingsMap(eventIds);

        Map<Long, UserShortDto> initiatorsMap = getInitiatorsMap(events);

        return events.stream()
                .map(event -> {
                    EventFullDto dto = eventMapper.toEventFullDto(event);
                    dto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0L));
                    dto.setRating(ratingsMap.getOrDefault(event.getId(), 0.0));
                    dto.setInitiator(initiatorsMap.get(event.getInitiatorId()));
                    return dto;
                })
                .toList();
    }

    private List<EventFullDto> enrichFullDtosWithoutRating(
            List<Event> events) {

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();

        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(eventIds);
        Map<Long, UserShortDto> initiatorsMap = getInitiatorsMap(events);

        return events.stream()
                .map(event -> {
                    EventFullDto dto = eventMapper.toEventFullDto(event);
                    dto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0L));
                    dto.setInitiator(initiatorsMap.get(event.getInitiatorId()));
                    dto.setRating(0.0);
                    return dto;
                })
                .toList();
    }

    private List<EventShortDto> enrichShortDtos(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyList();

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(eventIds);
        Map<Long, Integer> ratingsMap = getRatingsMap(eventIds);

        Map<Long, UserShortDto> initiatorsMap = getInitiatorsMap(events);

        return events.stream()
                .map(event -> {
                    EventShortDto shortDto = eventMapper.toEventShortDto(event);
                    shortDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0L));
                    shortDto.setRating(ratingsMap.getOrDefault(event.getId(), 0.0));
                    shortDto.setInitiator(initiatorsMap.get(event.getInitiatorId()));
                    return shortDto;
                })
                .toList();
    }

    private Map<Long, Double> getRecommendationRatingsMap(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            return analyzerClient.getInteractionsCount(eventIds)
                    .stream()
                    .collect(Collectors.toMap(
                            RecommendedEventProto::getEventId,
                            RecommendedEventProto::getScore
                    ));
        } catch (Exception e) {
            log.warn(
                    "Analyzer is not available, returning zero ratings: {}",
                    e.getMessage()
            );
            return Collections.emptyMap();
        }
    }

    private List<EventShortDto> enrichShortDtosWithoutRating(
            List<Event> events) {

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();

        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(eventIds);
        Map<Long, UserShortDto> initiatorsMap = getInitiatorsMap(events);

        return events.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toEventShortDto(event);
                    dto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0L));
                    dto.setInitiator(initiatorsMap.get(event.getInitiatorId()));
                    dto.setRating(0.0);
                    return dto;
                })
                .toList();
    }

    private Event getEventByIdOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(
                        () -> new NotFoundException("Событие с id - " + eventId + " не найдено")
                );
    }

    private Category getCategoryByIdOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(
                        () -> new NotFoundException("Категория с id - " + categoryId + " не найдена")
                );
    }

    private Map<Long, UserShortDto> getInitiatorsMap(List<Event> events) {

        List<Long> userIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .toList();

        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return userClient.getUsers(userIds)
                .stream()
                .collect(Collectors.toMap(
                        UserDto::getId,
                        user -> new UserShortDto(
                                user.getId(),
                                user.getName()
                        )
                ));
    }
}