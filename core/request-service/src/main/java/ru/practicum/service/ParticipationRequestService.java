package ru.practicum.service;

import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

/**
 * Сервис для управления заявками пользователей на участие в событиях.
 */
public interface ParticipationRequestService {

    /**
     * Создаёт заявку пользователя на участие в событии.
     *
     * @param userId  идентификатор пользователя
     * @param eventId идентификатор события
     * @return созданная заявка на участие
     */
    ParticipationRequestDto addParticipationRequest(Long userId, Long eventId);

    /**
     * Отменяет заявку пользователя на участие в событии.
     *
     * @param userId    идентификатор пользователя
     * @param requestId идентификатор заявки
     * @return отменённая заявка
     */
    ParticipationRequestDto cancelParticipationRequest(Long userId, Long requestId);

    /**
     * Возвращает все заявки текущего пользователя.
     *
     * @param userId идентификатор пользователя
     * @return список заявок пользователя
     */
    List<ParticipationRequestDto> getCurrentUserRequests(Long userId);

    /**
     * Возвращает заявки на участие в указанном событии.
     *
     * @param userId  идентификатор инициатора события
     * @param eventId идентификатор события
     * @return список заявок на участие
     */
    List<ParticipationRequestDto> getRequestsByEvent(Long userId, Long eventId);

    /**
     * Изменяет статус заявок на участие в событии.
     *
     * @param userId        идентификатор инициатора события
     * @param eventId       идентификатор события
     * @param updateRequest данные для изменения статусов заявок
     * @return результат изменения статусов заявок
     */
    EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequest);

    /**
     * Возвращает количество подтверждённых заявок на событие.
     *
     * @param eventId идентификатор события
     * @return количество подтверждённых заявок
     */
    Long getConfirmedCount(Long eventId);

    /**
     * Возвращает количество подтверждённых заявок для нескольких событий.
     *
     * @param eventIds идентификаторы событий
     * @return карта, где ключ — идентификатор события,
     * значение — количество подтверждённых заявок
     */
    Map<Long, Long> getConfirmedCounts(List<Long> eventIds);

    /**
     * Проверяет, является ли пользователь подтверждённым участником события.
     *
     * @param userId  идентификатор пользователя
     * @param eventId идентификатор события
     * @return {@code true}, если пользователь является подтверждённым участником
     */
    boolean isConfirmedParticipant(Long userId, Long eventId);
}
