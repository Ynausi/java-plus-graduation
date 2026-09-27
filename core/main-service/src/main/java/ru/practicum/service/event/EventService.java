package ru.practicum.service.event;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.dto.event.*;
import ru.practicum.dto.requests.EventRequestStatusUpdateRequest;
import ru.practicum.dto.requests.EventRequestStatusUpdateResult;
import ru.practicum.dto.requests.ParticipationRequestDto;

import java.util.List;

public interface EventService {
    List<EventShortDto> getEventsByUser(Long userId, Integer from, Integer size);

    List<ParticipationRequestDto> getRequestsByEvent(Long userId, Long eventId);

    EventFullDto createEvent(Long userId, NewEventDto newEventDto);

    EventFullDto getEventById(Long userId, Long eventId);

    EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest);

    EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequest);

    List<EventFullDto> getEventsByAdmin(EventSearchFilterAdmin filter,
                                        Integer from,
                                        Integer size);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request);

    List<EventShortDto> getEventsPublic(EventSearchFilterPublic filter,
                                        Integer from,
                                        Integer size,
                                        HttpServletRequest request);

    EventFullDto getPublicEventById(Long eventId, HttpServletRequest request);

    List<EventFullDto> getFavoriteEvents(Long userId);

    List<EventShortDto> getTopEventsByRating(Integer limit, String order);
}