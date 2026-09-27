package ru.practicum.model;

import lombok.Getter;

@Getter
public enum ReactionType {
    LIKE(1),
    DISLIKE(-1);

    private final Integer weight;

    ReactionType(int weight) {
        this.weight = weight;
    }

    public boolean isPositive() {
        return this == LIKE;
    }

    public boolean isNegative() {
        return this == DISLIKE;
    }
}