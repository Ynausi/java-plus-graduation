package ru.practicum.service;

import ru.practicum.model.*;

import java.time.LocalDateTime;

public abstract class BaseServiceTest {

    protected User makeUniqueUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("user" + id);
        user.setEmail("user_email" + id + "@mail.ru");
        return user;
    }

    protected Category makeCategory(Long id) {
        Category category = new Category();
        category.setId(id);
        category.setName("Category" + id);
        return category;
    }

    protected Event makeEvent(Long id, User initiator, Long categoryId) {
        Event event = new Event();
        event.setId(id);
        event.setAnnotation("Test annotation " + id);
        event.setDescription("Test description " + id);
        event.setInitiator(initiator);
        event.setCategory(makeCategory(categoryId));
        event.setEventDate(LocalDateTime.now().plusDays(7));
        event.setPaid(false);
        event.setParticipantLimit(0);
        event.setRequestModeration(false);
        event.setTitle("Test event title " + id);
        event.setEventState(EventState.PUBLISHED);
        event.setCreatedOn(LocalDateTime.now());
        return event;
    }

    protected EventReaction makeReaction(Long id, User reactor, Event event, ReactionType type) {
        EventReaction savedReaction = new EventReaction();
        savedReaction.setId(id);
        savedReaction.setReactor(reactor);
        savedReaction.setEvent(event);
        savedReaction.setReactionType(type);
        return savedReaction;
    }
}
