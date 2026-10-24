package ru.practicum.repository.event;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Event;
import ru.practicum.repository.event.EventQuerydslRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с сущностями событий.
 */
public interface EventRepository extends JpaRepository<Event, Long>, EventQuerydslRepository {

    /**
     * Возвращает наиболее раннюю дату создания среди указанных событий.
     *
     * @param eventIds идентификаторы событий
     * @return наиболее ранняя дата создания события
     */
    @Query("SELECT MIN(e.createdOn) FROM Event e WHERE e.id IN :eventIds")
    Optional<LocalDateTime> findEarliestCreatedOnByEventIds(@Param("eventIds") List<Long> eventIds);

    /**
     * Возвращает события, созданные указанным пользователем.
     *
     * @param userId   идентификатор инициатора события
     * @param pageable параметры пагинации
     * @return список событий пользователя
     */
    List<Event> findAllByInitiatorId(Long userId, Pageable pageable);

    /**
     * Возвращает событие по идентификатору и идентификатору инициатора.
     *
     * @param eventId идентификатор события
     * @param userId  идентификатор инициатора
     * @return найденное событие
     */
    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    /**
     * Проверяет наличие событий в указанной категории.
     *
     * @param categoryId идентификатор категории
     * @return {@code true}, если в категории существует хотя бы одно событие
     */
    Boolean existsByCategoryId(Long categoryId);

    /**
     * Возвращает события, созданные указанными пользователями.
     *
     * @param userIds идентификаторы инициаторов
     * @return список событий
     */
    List<Event> findAllByInitiatorIdIn(List<Long> userIds);
}