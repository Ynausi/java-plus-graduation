package ru.practicum.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.practicum.client.EventClient;
import ru.practicum.client.UserClient;
import ru.practicum.dto.EventForRequestDto;
import ru.practicum.dto.EventState;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.mapper.ParticipationRequestMapper;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.stats.CollectorClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipationRequestServiceImplTest {

    @Mock
    private ParticipationRequestRepository requestRepository;
    @Mock
    private ParticipationRequestMapper mapper;
    @Mock
    private UserClient userClient;
    @Mock
    private EventClient eventClient;
    @Mock
    private CollectorClient collectorClient;

    @BeforeEach
    void openTransactionSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearTransactionSynchronization() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void registrationIsPublishedAfterCommitAndCollectorFailureDoesNotBreakRequest() {
        EventForRequestDto event = new EventForRequestDto(20L, 30L, EventState.PUBLISHED, 0, false);
        ParticipationRequest saved = new ParticipationRequest();
        ParticipationRequestDto dto = new ParticipationRequestDto();
        when(eventClient.getEvent(20L)).thenReturn(event);
        when(mapper.toEntity(10L, 20L)).thenReturn(saved);
        when(requestRepository.save(saved)).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(dto);

        ParticipationRequestServiceImpl service = new ParticipationRequestServiceImpl(
                requestRepository, mapper, userClient, eventClient, collectorClient);

        assertSame(dto, service.addParticipationRequest(10L, 20L));
        verify(collectorClient, never()).collectUserAction(10L, 20L, ActionTypeProto.ACTION_REGISTER);

        doThrow(new IllegalStateException("Collector unavailable")).when(collectorClient)
                .collectUserAction(10L, 20L, ActionTypeProto.ACTION_REGISTER);
        List<TransactionSynchronization> callbacks = TransactionSynchronizationManager.getSynchronizations();
        assertEqualsOne(callbacks);
        assertDoesNotThrow(() -> callbacks.forEach(TransactionSynchronization::afterCommit));
        verify(collectorClient).collectUserAction(10L, 20L, ActionTypeProto.ACTION_REGISTER);
    }

    private void assertEqualsOne(List<TransactionSynchronization> callbacks) {
        org.junit.jupiter.api.Assertions.assertEquals(1, callbacks.size());
    }
}
