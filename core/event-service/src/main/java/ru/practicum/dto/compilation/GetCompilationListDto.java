package ru.practicum.dto.compilation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GetCompilationListDto {

    private Integer size = 10;

    private Boolean pinned = false;

    private Integer from = 0;
}
