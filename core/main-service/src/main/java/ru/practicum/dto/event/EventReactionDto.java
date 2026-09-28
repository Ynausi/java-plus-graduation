package ru.practicum.dto.event;

import lombok.*;
import ru.practicum.model.ReactionType;

import java.time.LocalDateTime;

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