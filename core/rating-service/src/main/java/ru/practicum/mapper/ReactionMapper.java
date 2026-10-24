package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.EventReactionDto;
import ru.practicum.model.EventReaction;
import ru.practicum.model.ReactionType;

@Mapper(componentModel = "spring")
public interface ReactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "reactorId", source = "userId")
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    EventReaction toEntity(
            Long userId,
            Long eventId,
            ReactionType reactionType
    );

    @Mapping(target = "reactor", source = "reactorId")
    @Mapping(target = "event", source = "eventId")
    EventReactionDto toDto(EventReaction reaction);
}
