package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.dto.EventForRequestDto;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.dto.UserDto;
import ru.practicum.model.ParticipationRequest;

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
    default UserDto mapToUser(Long userId) {
        if (userId == null) return null;
        UserDto user = new UserDto();
        user.setId(userId);
        return user;
    }

    @Named("mapToEvent")
    default EventForRequestDto mapToEvent(Long eventId) {
        if (eventId == null) return null;
        EventForRequestDto event = new EventForRequestDto();
        event.setId(eventId);
        return event;
    }
}
