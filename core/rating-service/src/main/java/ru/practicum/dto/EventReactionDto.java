package ru.practicum.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.practicum.model.ReactionType;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventReactionDto {
    private Long reactor;
    private Long event;
    private ReactionType reactionType;
    private LocalDateTime createdAt;
}