package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.dto.event.*;
import ru.practicum.model.*;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class, UserMapper.class})
public interface EventMapper {

    @Mapping(target = "eventDate", source = "eventDate")
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "rating", ignore = true)
    EventShortDto toEventShortDto(Event event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "participationRequests", ignore = true)
    @Mapping(target = "initiator", ignore = true)
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

    @Mapping(target = "reactor", source = "reactor.id")
    @Mapping(target = "event", source = "event.id")
    EventReactionDto toReactionDto(EventReaction reaction);

    @Named("mapToUser")
    default User mapToUser(Long userId) {
        if (userId == null) return null;
        User user = new User();
        user.setId(userId);
        return user;
    }

    @Named("mapToEvent")
    default Event mapToEvent(Long eventId) {
        if (eventId == null) return null;
        Event event = new Event();
        event.setId(eventId);
        return event;
    }
}