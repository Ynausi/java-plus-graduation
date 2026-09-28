package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.compilation.CompilationRequest;
import ru.practicum.dto.compilation.CompilationResponse;
import ru.practicum.model.Compilation;

@Mapper(componentModel = "spring", uses = EventMapper.class)
public interface CompilationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    Compilation toModel(CompilationRequest compilationRequest);

    CompilationResponse toResponse(Compilation compilation);
}
