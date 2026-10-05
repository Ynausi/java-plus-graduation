package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.dto.*;
import ru.practicum.model.Event;
import ru.practicum.model.EventReaction;
import ru.practicum.model.Location;
import ru.practicum.model.ReactionType;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "eventDate", source = "eventDate")
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "rating", ignore = true)
    EventShortDto toEventShortDto(Event event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "participationRequests", ignore = true)
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "eventState", ignore = true)
    @Mapping(target = "eventReactions", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    Event toEvent(NewEventDto newEventDto);

    @Mapping(target = "state", source = "eventState")
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "rating", ignore = true)
    EventFullDto toEventFullDto(Event event);

    LocationDto toLocationDto(Location location);

    Location toLocation(LocationDto locationDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "reactor", source = "userId", qualifiedByName = "mapToUser")
    @Mapping(target = "event", source = "eventId", qualifiedByName = "mapToEvent")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    EventReaction toReaction(Long userId, Long eventId, ReactionType reactionType);

    @Mapping(target = "reactorId", source = "reactor")
    @Mapping(target = "event", source = "event.id")
    EventReactionDto toReactionDto(EventReaction reaction);

    @Mapping(target = "initiatorId",source = "initiatorId")
    @Mapping(target = "state",source = "eventState")
    EventForRequestDto toEventForRequestDto(Event event);

    @Named("mapToEvent")
    default Event mapToEvent(Long eventId) {
        if (eventId == null) return null;
        Event event = new Event();
        event.setId(eventId);
        return event;
    }
}