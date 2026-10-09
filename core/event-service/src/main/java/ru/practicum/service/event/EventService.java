package ru.practicum.service.event;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.dto.*;

import java.util.List;
import java.util.Map;

/**
 * Сервис для управления событиями.
 * Предоставляет пользовательские, административные,
 * публичные и внутренние операции с событиями.
 */
public interface EventService {
    /**
     * Возвращает события, созданные указанным пользователем.
     *
     * @param userId идентификатор пользователя
     * @param from   количество элементов, которые необходимо пропустить
     * @param size   количество элементов в выборке
     * @return список событий пользователя
     */
    List<EventShortDto> getEventsByUser(Long userId, Integer from, Integer size);

    /**
     * Создаёт новое событие.
     *
     * @param userId      идентификатор пользователя
     * @param newEventDto данные нового события
     * @return созданное событие
     */
    EventFullDto createEvent(Long userId, NewEventDto newEventDto);

    /**
     * Возвращает событие пользователя по идентификатору.
     *
     * @param userId  идентификатор пользователя
     * @param eventId идентификатор события
     * @return полное представление события
     */
    EventFullDto getEventById(Long userId, Long eventId);

    /**
     * Возвращает данные события для внутреннего взаимодействия сервисов.
     *
     * @param eventId идентификатор события
     * @return данные события
     */
    EventForRequestDto getEventById(Long eventId);

    /**
     * Изменяет событие его инициатором.
     *
     * @param userId                 идентификатор пользователя
     * @param eventId                идентификатор события
     * @param updateEventUserRequest данные для изменения события
     * @return обновлённое событие
     */
    EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest);

    /**
     * Возвращает события по административным параметрам поиска.
     *
     * @param filter параметры фильтрации
     * @param from   количество элементов, которые необходимо пропустить
     * @param size   количество элементов в выборке
     * @return список найденных событий
     */
    List<EventFullDto> getEventsByAdmin(EventSearchFilterAdmin filter,
                                        Integer from,
                                        Integer size);

    /**
     * Изменяет событие администратором.
     *
     * @param eventId идентификатор события
     * @param request данные для изменения события
     * @return обновлённое событие
     */
    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request);

    /**
     * Выполняет публичный поиск событий.
     *
     * @param filter  параметры фильтрации
     * @param from    количество элементов, которые необходимо пропустить
     * @param size    количество элементов в выборке
     * @param request HTTP-запрос для учёта статистики
     * @return список найденных событий
     */
    List<EventShortDto> getEventsPublic(EventSearchFilterPublic filter,
                                        Integer from,
                                        Integer size,
                                        HttpServletRequest request);

    /**
     * Возвращает опубликованное событие по идентификатору.
     *
     * @param eventId идентификатор события
     * @param request HTTP-запрос для учёта просмотра
     * @return полное представление события
     */
    EventFullDto getPublicEventById(Long eventId, HttpServletRequest request);

    /**
     * Возвращает полные представления событий по идентификаторам.
     *
     * @param eventIds идентификаторы событий
     * @return список полных представлений событий
     */
    List<EventFullDto> getFullEventsByIds(List<Long> eventIds);

    /**
     * Возвращает краткие представления событий по идентификаторам.
     *
     * @param eventIds идентификаторы событий
     * @return список кратких представлений событий
     */
    List<EventShortDto> getShortEventsByIds(List<Long> eventIds);

    /**
     * Возвращает соответствие событий и их инициаторов.
     *
     * @param userIds идентификаторы инициаторов
     * @return карта, где ключ — идентификатор события,
     * значение — идентификатор инициатора
     */
    Map<Long, Long> getEventOwnersByInitiatorIds(List<Long> userIds);
}