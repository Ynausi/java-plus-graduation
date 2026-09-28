package ru.practicum.dto.users;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRatingStatsDto {
    private Long userId;
    private Long likes;
    private Long dislikes;
}