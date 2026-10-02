package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.dto.requests.ParticipationRequestDto;
import ru.practicum.model.Event;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.model.User;

@Mapper(componentModel = "spring")
public interface ParticipationRequestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "requester", source = "requesterId", qualifiedByName = "mapToUser")
    @Mapping(target = "event", source = "eventId", qualifiedByName = "mapToEvent")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "created", ignore = true)
    ParticipationRequest toEntity(Long requesterId, Long eventId);

    @Mapping(target = "requester", source = "requester.id")
    @Mapping(target = "event", source = "event.id")
    ParticipationRequestDto toDto(ParticipationRequest request);

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
