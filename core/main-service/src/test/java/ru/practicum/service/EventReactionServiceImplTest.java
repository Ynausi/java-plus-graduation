package ru.practicum.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.dto.event.EventReactionDto;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.*;
import ru.practicum.repository.EventReactionRepository;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.repository.UsersRepository;
import ru.practicum.repository.event.EventRepository;
import ru.practicum.service.event.EventReactionServiceImpl;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventReactionServiceImplTest extends BaseServiceTest {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventReactionRepository eventReactionRepository;

    @Mock
    private ParticipationRequestRepository requestRepository;

    @Mock
    private EventMapper eventMapper;

    @InjectMocks
    private EventReactionServiceImpl eventReactionService;

    private User user;
    private User initiator;
    private Event event;
    private EventReaction reaction;
    private EventReactionDto expectedDto;

    private void prepareData(Long userId, Long eventId, ReactionType typeEntity, ReactionType typeDto) {
        user = makeUniqueUser(userId);
        initiator = makeUniqueUser(2L);
        event = makeEvent(eventId, initiator, 1L);

        reaction = makeReaction(1L, user, event, typeEntity);
        reaction.setUpdatedAt(LocalDateTime.now());

        expectedDto = new EventReactionDto();
        expectedDto.setReactionType(typeDto);
        expectedDto.setReactor(userId);
        expectedDto.setEvent(eventId);
    }

    @ParameterizedTest
    @CsvSource({
            "LIKE",
            "DISLIKE"
    })
    void test_addReaction_shouldSuccess(ReactionType reactionType) {
        Long userId = 1L;
        Long eventId = 1L;

        prepareData(userId, eventId, reactionType, reactionType);

        when(usersRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventReactionRepository.findByReactorIdAndEventId(userId, eventId))
                .thenReturn(Optional.empty());
        when(requestRepository.existsByRequesterIdAndEventIdAndStatus(
                userId, eventId, RequestStatus.CONFIRMED))
                .thenReturn(true);
        when(eventMapper.toReaction(eq(userId), eq(eventId), eq(reactionType)))
                .thenReturn(reaction);
        when(eventReactionRepository.save(any(EventReaction.class))).thenReturn(reaction);
        when(eventMapper.toReactionDto(any(EventReaction.class))).thenReturn(expectedDto);

        EventReactionDto result = eventReactionService.addReaction(userId, eventId, reactionType);

        assertThat(result).isNotNull();
        assertThat(result.getReactionType()).isEqualTo(reactionType);
        verify(eventReactionRepository, times(1)).save(argThat(savedReaction ->
                savedReaction.getReactor().getId().equals(userId) &&
                        savedReaction.getReactionType() == reactionType
        ));
    }

    @ParameterizedTest
    @CsvSource({
            "LIKE",
            "DISLIKE"
    })
    void test_addReaction_whenUserNotParticipant_shouldThrowException(ReactionType reactionType) {
        Long userId = 1L;
        Long eventId = 1L;

        when(usersRepository.findById(userId)).thenReturn(Optional.of(makeUniqueUser(userId)));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(makeEvent(eventId, makeUniqueUser(2L), 1L)));
        when(requestRepository.existsByRequesterIdAndEventIdAndStatus(
                userId, eventId, RequestStatus.CONFIRMED))
                .thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> eventReactionService.addReaction(userId, eventId, reactionType));

        verify(eventReactionRepository, never()).save(any());
    }

    @Test
    void test_addReaction_whenExistsDifferentReaction_shouldUpdate() {
        Long userId = 1L;
        Long eventId = 1L;
        ReactionType oldType = ReactionType.DISLIKE;
        ReactionType newType = ReactionType.LIKE;

        prepareData(userId, eventId, oldType, newType);

        when(usersRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventReactionRepository.findByReactorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(reaction));
        when(requestRepository.existsByRequesterIdAndEventIdAndStatus(
                userId, eventId, RequestStatus.CONFIRMED))
                .thenReturn(true);
        when(eventMapper.toReactionDto(any(EventReaction.class))).thenReturn(expectedDto);

        EventReactionDto result = eventReactionService.addReaction(userId, eventId, newType);

        assertThat(result).isNotNull();
        assertThat(result.getReactionType()).isEqualTo(newType);

        verify(eventReactionRepository, never()).save(any());
    }

    @Test
    void test_addReaction_whenSameReactionExists_shouldThrowConflictException() {
        Long userId = 1L;
        Long eventId = 1L;
        ReactionType reactionType = ReactionType.LIKE;

        prepareData(userId, eventId, reactionType, reactionType);

        when(usersRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(requestRepository.existsByRequesterIdAndEventIdAndStatus(
                userId, eventId, RequestStatus.CONFIRMED))
                .thenReturn(true);
        when(eventReactionRepository.findByReactorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(reaction));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventReactionService.addReaction(userId, eventId, reactionType));

        assertThat(exception.getMessage()).isEqualTo("Already reacted this event");
        verify(eventReactionRepository, never()).save(any(EventReaction.class));
        verify(eventMapper, never()).toReactionDto(any(EventReaction.class));
    }

    @Test
    void test_addReaction_whenUserNotFound_shouldThrowNotFoundException() {
        Long userId = 999L;
        Long eventId = 1L;
        ReactionType reactionType = ReactionType.LIKE;

        when(usersRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> eventReactionService.addReaction(userId, eventId, reactionType));

        verify(eventReactionRepository, never()).findByReactorIdAndEventId(any(), any());
        verify(eventReactionRepository, never()).save(any());
    }

    @Test
    void test_addReaction_whenEventNotFound_shouldThrowNotFoundException() {
        Long userId = 1L;
        Long eventId = 999L;
        ReactionType reactionType = ReactionType.LIKE;

        when(usersRepository.findById(userId)).thenReturn(Optional.of(makeUniqueUser(userId)));
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> eventReactionService.addReaction(userId, eventId, reactionType));

        verify(eventReactionRepository, never()).findByReactorIdAndEventId(any(), any());
        verify(eventReactionRepository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({
            "LIKE",
            "DISLIKE"
    })
    void test_deleteReaction_shouldSuccess(ReactionType reactionType) {
        Long userId = 1L;
        Long eventId = 1L;

        prepareData(userId, eventId, reactionType, null);

        when(usersRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventReactionRepository.findByReactorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(reaction));

        eventReactionService.deleteReaction(userId, eventId, reactionType);

        verify(eventReactionRepository, times(1)).delete(reaction);
    }

    @Test
    void test_deleteReaction_whenNoReaction_shouldThrowNotFoundException() {
        Long userId = 1L;
        Long eventId = 1L;
        ReactionType reactionType = ReactionType.LIKE;

        when(usersRepository.findById(userId)).thenReturn(Optional.of(makeUniqueUser(userId)));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(makeEvent(eventId, makeUniqueUser(2L), 1L)));
        when(eventReactionRepository.findByReactorIdAndEventId(userId, eventId))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventReactionService.deleteReaction(userId, eventId, reactionType));

        assertThat(exception.getMessage()).contains("Reaction LIKE not found for this event");
        verify(eventReactionRepository, never()).delete(any());
    }

    @Test
    void test_deleteReaction_whenReactionTypeMismatch_shouldThrowNotFoundException() {
        Long userId = 1L;
        Long eventId = 1L;
        ReactionType existingType = ReactionType.DISLIKE;
        ReactionType deleteType = ReactionType.LIKE;

        prepareData(userId, eventId, existingType, null);

        when(usersRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventReactionRepository.findByReactorIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(reaction));

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventReactionService.deleteReaction(userId, eventId, deleteType));

        assertThat(exception.getMessage()).contains("Reaction LIKE not found for this event");
        verify(eventReactionRepository, never()).delete(any());
    }

    @Test
    void test_deleteReaction_whenUserNotFound_shouldThrowNotFoundException() {
        Long userId = 999L;
        Long eventId = 1L;
        ReactionType reactionType = ReactionType.LIKE;

        when(usersRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> eventReactionService.deleteReaction(userId, eventId, reactionType));

        verify(eventReactionRepository, never()).findByReactorIdAndEventId(any(), any());
    }

    @Test
    void test_deleteReaction_whenEventNotFound_shouldThrowNotFoundException() {
        Long userId = 1L;
        Long eventId = 999L;
        ReactionType reactionType = ReactionType.LIKE;

        when(usersRepository.findById(userId)).thenReturn(Optional.of(makeUniqueUser(userId)));
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> eventReactionService.deleteReaction(userId, eventId, reactionType));

        verify(eventReactionRepository, never()).findByReactorIdAndEventId(any(), any());
    }
}