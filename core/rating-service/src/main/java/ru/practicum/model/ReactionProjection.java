package ru.practicum.model;

public interface ReactionProjection {
    Long getEventId();

    ReactionType getReaction();
}
