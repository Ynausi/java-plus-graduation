package ru.practicum.repository.event;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import ru.practicum.dto.event.EventSearchFilterAdmin;
import ru.practicum.dto.event.EventSearchFilterPublic;
import ru.practicum.dto.event.PublicEventSort;
import ru.practicum.model.Event;
import ru.practicum.model.EventState;
import ru.practicum.model.QEvent;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EventRepositoryImpl implements EventQuerydslRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Event> searchAdmin(EventSearchFilterAdmin filter, Pageable pageable) {
        QEvent qEvent = QEvent.event;

        BooleanBuilder where = new BooleanBuilder();

        if (filter.users() != null && !filter.users().isEmpty()) {
            where.and(qEvent.initiator.id.in(filter.users()));
        }
        if (filter.states() != null && !filter.states().isEmpty()) {
            where.and(qEvent.eventState.in(filter.states()));
        }
        if (filter.categories() != null && !filter.categories().isEmpty()) {
            where.and(qEvent.category.id.in(filter.categories()));
        }
        if (filter.rangeStart() != null) {
            where.and(qEvent.eventDate.goe(filter.rangeStart()));
        }
        if (filter.rangeEnd() != null) {
            where.and(qEvent.eventDate.loe(filter.rangeEnd()));
        }

        return queryFactory.selectFrom(qEvent)
                .where(where)
                .orderBy(orderSpecifiersFrom(pageable.getSort(), qEvent).toArray(OrderSpecifier[]::new))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<Event> searchPublic(EventSearchFilterPublic filter, Pageable pageable) {
        QEvent e = QEvent.event;

        BooleanBuilder where = new BooleanBuilder();
        where.and(e.eventState.eq(EventState.PUBLISHED));

        if (filter.text() != null && !filter.text().isBlank()) {
            where.and(
                    e.annotation.containsIgnoreCase(filter.text())
                            .or(e.description.containsIgnoreCase(filter.text()))
            );
        }

        if (filter.categories() != null && !filter.categories().isEmpty()) {
            where.and(e.category.id.in(filter.categories()));
        }

        if (filter.users() != null && !filter.users().isEmpty()) {
            where.and(e.initiator.id.in(filter.users()));
        }

        if (filter.paid() != null) {
            where.and(e.paid.eq(filter.paid()));
        }

        if (filter.rangeStart() != null) {
            where.and(e.eventDate.goe(filter.rangeStart()));
        }

        if (filter.rangeEnd() != null) {
            where.and(e.eventDate.loe(filter.rangeEnd()));
        }

        List<OrderSpecifier<?>> orderSpecifiers = orderSpecifiersFrom(pageable.getSort(), e);

        if (orderSpecifiers.isEmpty()) {
            orderSpecifiers.add(e.eventDate.asc());
        }

        if (filter.sort() == PublicEventSort.VIEWS) {
            orderSpecifiers.clear();
            orderSpecifiers.add(e.id.asc());
        }

        return queryFactory.selectFrom(e)
                .where(where)
                .orderBy(orderSpecifiers.toArray(OrderSpecifier[]::new))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    private List<OrderSpecifier<?>> orderSpecifiersFrom(Sort sort, QEvent e) {
        List<OrderSpecifier<?>> specifiers = new ArrayList<>();
        if (sort == null) return specifiers;

        for (Sort.Order order : sort) {
            boolean asc = order.isAscending();
            String property = order.getProperty();

            switch (property) {
                case "id" -> specifiers.add(asc ? e.id.asc() : e.id.desc());
                case "eventDate" -> specifiers.add(asc ? e.eventDate.asc() : e.eventDate.desc());
                case "createdOn" -> specifiers.add(asc ? e.createdOn.asc() : e.createdOn.desc());
                default -> specifiers.add(e.id.asc());
            }
        }
        return specifiers;
    }
}
