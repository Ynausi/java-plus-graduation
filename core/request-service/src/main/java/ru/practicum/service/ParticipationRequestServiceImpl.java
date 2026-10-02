package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.UserClient;
import ru.practicum.client.EventClient;
import ru.practicum.dto.EventForRequestDto;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.dto.UserDto;
import ru.practicum.exceptions.ConflictException;
import ru.practicum.exceptions.RequestNotFoundException;
import ru.practicum.mapper.ParticipationRequestMapper;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.model.RequestStatus;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.ParticipationRequestDto;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final ParticipationRequestMapper mapper;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Override
    @Transactional
    public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
        userClient.getUser(userId);

        EventForRequestDto event = eventClient.getEvent(eventId);

        validateRequest(event, userId);

        ParticipationRequest request = mapper.toEntity(userId, eventId);

        if (event.getParticipantLimit() == 0 || Boolean.FALSE.equals(event.getRequestModeration())) {
            request.setStatus(RequestStatus.CONFIRMED);
        }

        return mapper.toDto(requestRepository.save(request));
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelParticipationRequest(Long userId, Long requestId) {
        userClient.getUser(userId);

        ParticipationRequest request = requestRepository.findById(requestId).orElseThrow(() ->
                new RequestNotFoundException(String.format("Request with id=%s was not found", requestId)));

        if (!Objects.equals(request.getRequester(), userId)) {
            throw new ConflictException(
                    "Only the requester can cancel the request"
            );
        }

        request.setStatus(RequestStatus.CANCELED);

        return mapper.toDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getCurrentUserRequests(Long userId) {
        userClient.getUser(userId);

        return requestRepository.findAllByRequesterId(userId).stream()
                .map(mapper::toDto)
                .toList();
    }

    private void validateRequest(Event event, Long userId) {
        Integer eventParticipationLimit = event.getParticipantLimit();
        Long currentConfirmedRequests =
                requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);

        if (Objects.equals(userId, event.getInitiator().getId())) {
            throw new ConflictException(
                    "Event initiator cannot request participation in their own event"
            );
        }
        if (!EventState.PUBLISHED.equals(event.getEventState())) {
            throw new ConflictException(
                    "Cannot participate in unpublished event. Current status: " + event.getEventState()
            );
        }
        if (eventParticipationLimit > 0 && currentConfirmedRequests >= eventParticipationLimit) {
            throw new ConflictException(
                    String.format("Event participant limit has been reached. Limit: %d, Current: %d",
                            event.getParticipantLimit(), currentConfirmedRequests)
            );
        }

    }

    @Override
    public List<ParticipationRequestDto> getRequestsByEvent(Long userId, Long eventId) {
        getUserByIdOrThrow(userId);
        Event event = getEventByIdOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId))
            throw new NotFoundException("Пользователь не является инициатором этого события");
        return requestRepository.findAllByEventId(eventId).stream()
                .map(requestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId,
                                                              Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        getUserByIdOrThrow(userId);
        Event event = getEventByIdOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Пользователь не является инициатором этого события");
        }

        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit()) {
            throw new ConflictException("Лимит участников для данного события уже исчерпан");
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(updateRequest.getRequestIds());

        for (ParticipationRequest request : requests) {
            if (!request.getEvent().getId().equals(eventId)) {
                throw new BadRequestException("Запрос не относится к данному событию");
            }
            if (!request.getStatus().equals(RequestStatus.PENDING)) {
                throw new ConflictException("Статус можно менять только у заявок, находящихся в состоянии ожидания");
            }
        }

        List<ParticipationRequest> confirmedRequests = new ArrayList<>();
        List<ParticipationRequest> rejectedRequests = new ArrayList<>();
        RequestStatus targetStatus = updateRequest.getStatus();

        if (targetStatus == RequestStatus.REJECTED) {
            for (ParticipationRequest request : requests) {
                request.setStatus(RequestStatus.REJECTED);
                rejectedRequests.add(request);
            }
        } else if (targetStatus == RequestStatus.CONFIRMED) {
            for (ParticipationRequest request : requests) {
                if (event.getParticipantLimit() == 0 || confirmedCount < event.getParticipantLimit()) {
                    request.setStatus(RequestStatus.CONFIRMED);
                    confirmedRequests.add(request);
                    confirmedCount++;
                } else {
                    request.setStatus(RequestStatus.REJECTED);
                    rejectedRequests.add(request);
                }
            }
        }

        requestRepository.saveAll(requests);
        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequests.stream().map(requestMapper::toDto).toList())
                .rejectedRequests(rejectedRequests.stream().map(requestMapper::toDto).toList())

    }
