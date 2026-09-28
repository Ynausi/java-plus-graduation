package ru.practicum.dto.compilation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CompilationRequest {

    private Set<Long> events = new HashSet<>();

    private Boolean pinned = false;

    @NotBlank
    @Size(min = 1, max = 50)
    private String title;
}
