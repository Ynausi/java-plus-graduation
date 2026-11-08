package ru.practicum.analyzer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.model.UserInteraction;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserInteractionRepository;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private UserInteractionRepository interactionRepository;
    @Mock
    private EventSimilarityRepository similarityRepository;
    @InjectMocks
    private RecommendationService recommendationService;

    @Test
    void recommendationsUseOneInteractionQueryAndOneBatchPredictionQuery() {
        List<UserInteraction> interactions = List.of(
                UserInteraction.builder().userId(10L).eventId(11L).weight(1.0)
                        .updatedAt(Instant.parse("2026-01-02T00:00:00Z")).build(),
                UserInteraction.builder().userId(10L).eventId(12L).weight(0.8)
                        .updatedAt(Instant.parse("2026-01-01T00:00:00Z")).build()
        );
        EventSimilarity first = EventSimilarity.builder()
                .eventA(11L).eventB(21L).score(0.7).build();
        EventSimilarity second = EventSimilarity.builder()
                .eventA(12L).eventB(21L).score(0.6).build();

        when(interactionRepository.findByUserId(10L)).thenReturn(interactions);
        when(similarityRepository.findForEvents(anyList())).thenAnswer(invocation -> {
            List<Long> events = invocation.getArgument(0);
            if (events.contains(11L)) {
                return List.of(first, second);
            }
            if (events.contains(21L)) {
                return List.of(first, second);
            }
            return List.of();
        });

        List<RecommendedEventProto> result = recommendationService.getRecommendationsForUser(10L, 5);

        assertEquals(1, result.size());
        assertEquals(21L, result.get(0).getEventId());
        assertEquals((0.7 * 1.0 + 0.6 * 0.8) / (0.7 + 0.6),
                result.get(0).getScore(), 1e-9);
        verify(interactionRepository, times(1)).findByUserId(10L);
        verify(similarityRepository, times(2)).findForEvents(anyList());
        verify(similarityRepository, never()).findForEvent(anyLong());
    }
}
