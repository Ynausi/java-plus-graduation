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
    @Mapping(target = "requesterId", source = "requesterId")
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "created", ignore = true)
    ParticipationRequest toEntity(Long requesterId, Long eventId);

    @Mapping(target = "requester", source = "requesterId")
    @Mapping(target = "event", source = "eventId")
    ParticipationRequestDto toDto(ParticipationRequest request);

}
