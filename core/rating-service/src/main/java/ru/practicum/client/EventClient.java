package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.EventForRequestDto;
import ru.practicum.dto.EventFullDto;
import ru.practicum.dto.EventShortDto;

import java.util.List;
import java.util.Map;

/**
 * Feign-клиент для взаимодействия с внутренним API сервиса событий.
 */
@FeignClient(name = "event-service")
public interface EventClient {

    /**
     * Возвращает данные события, необходимые другим микросервисам.
     *
     * @param eventId идентификатор события
     * @return данные события
     */
    @GetMapping("/internal/events/{eventId}")
    EventForRequestDto getEvent(@PathVariable Long eventId);

    /**
     * Возвращает полные представления событий по их идентификаторам.
     *
     * @param eventIds идентификаторы событий
     * @return список полных представлений событий
     */
    @PostMapping("/internal/events/full/by-ids")
    List<EventFullDto> getFullEventsByIds(@RequestBody List<Long> eventIds);

    /**
     * Возвращает краткие представления событий по их идентификаторам.
     *
     * @param eventIds идентификаторы событий
     * @return список кратких представлений событий
     */
    @PostMapping("/internal/events/short/by-ids")
    List<EventShortDto> getShortEventsByIds(@RequestBody List<Long> eventIds);

    /**
     * Возвращает инициаторов событий для указанных пользователей.
     *
     * @param userIds идентификаторы пользователей
     * @return карта, где ключ — идентификатор события,
     * значение — идентификатор его инициатора
     */
    @PostMapping("/internal/events/owners/by-initiators")
    Map<Long, Long> getEventOwnersByInitiatorIds(@RequestBody List<Long> userIds);
}
