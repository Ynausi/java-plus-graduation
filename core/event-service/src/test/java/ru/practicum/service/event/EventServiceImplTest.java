package ru.practicum.service.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.client.RequestClient;
import ru.practicum.client.UserClient;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.Event;
import ru.practicum.model.EventState;
import ru.practicum.repository.category.CategoryRepository;
import ru.practicum.repository.event.EventRepository;
import ru.practicum.stats.AnalyzerClient;
import ru.practicum.stats.CollectorClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventMapper eventMapper;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserClient userClient;
    @Mock
    private RequestClient requestClient;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CollectorClient collectorClient;
    @Mock
    private AnalyzerClient analyzerClient;
    @InjectMocks
    private EventServiceImpl eventService;

    @Test
    void analyzerOutageReturnsEmptyRecommendations() {
        when(analyzerClient.getRecommendationsForUser(10L, 5))
                .thenThrow(new IllegalStateException("Analyzer unavailable"));

        assertTrue(eventService.getRecommendations(10L, 5).isEmpty());
    }

    @Test
    void collectorOutageDoesNotBreakValidatedLike() {
        Event event = new Event();
        event.setEventState(EventState.PUBLISHED);
        when(eventRepository.findById(20L)).thenReturn(Optional.of(event));
        when(requestClient.isConfirmedParticipant(10L, 20L)).thenReturn(true);
        doThrow(new IllegalStateException("Collector unavailable")).when(collectorClient)
                .collectUserAction(10L, 20L, ActionTypeProto.ACTION_LIKE);

        assertDoesNotThrow(() -> eventService.likeEvent(10L, 20L));
    }
}
