package ru.practicum.service.requests;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.requests.ParticipationRequestDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.ParticipationRequestMapper;
import ru.practicum.model.Event;
import ru.practicum.model.EventState;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.model.RequestStatus;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.repository.UsersRepository;
import ru.practicum.repository.event.EventRepository;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UsersRepository usersRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestMapper mapper;

    @Override
    @Transactional
    public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
        usersRepository.findByIdOrThrow(userId);

        Event event = eventRepository.findById(eventId).orElseThrow(() ->
                new NotFoundException(String.format("Event with id=%s was not found", eventId)));

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
        usersRepository.findByIdOrThrow(userId);

        ParticipationRequest request = requestRepository.findById(requestId).orElseThrow(() ->
                new NotFoundException(String.format("Request with id=%s was not found", requestId)));

        if (!Objects.equals(request.getRequester().getId(), userId)) {
            throw new ConflictException(
                    "Only the requester can cancel the request"
            );
        }

        request.setStatus(RequestStatus.CANCELED);

        return mapper.toDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getCurrentUserRequests(Long userId) {
        usersRepository.findByIdOrThrow(userId);

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
}
