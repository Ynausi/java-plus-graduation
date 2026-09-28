package ru.practicum.dto.compilation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.practicum.dto.event.EventShortDto;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CompilationResponse {

    private Long id;
    private List<EventShortDto> events = new ArrayList<>();
    private Boolean pinned;
    private String title;

}
