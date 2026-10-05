package ru.practicum.dto;

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