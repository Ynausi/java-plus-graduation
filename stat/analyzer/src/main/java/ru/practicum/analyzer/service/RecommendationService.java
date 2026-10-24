package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.model.UserInteraction;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserInteractionRepository;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private static final int RECENT_INTERACTIONS_LIMIT = 20;
    private static final int NEIGHBOURS_LIMIT = 10;

    private final UserInteractionRepository interactionRepository;
    private final EventSimilarityRepository similarityRepository;

    public List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        if (maxResults <= 0) {
            return List.of();
        }

        List<UserInteraction> allInteractions = interactionRepository.findByUserId(userId);
        if (allInteractions.isEmpty()) {
            return List.of();
        }

        Map<Long, UserInteraction> interactionsByEvent = allInteractions.stream()
                .collect(Collectors.toMap(UserInteraction::getEventId, Function.identity()));

        List<Long> recentEvents = interactionRepository
                .findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .limit(RECENT_INTERACTIONS_LIMIT)
                .map(UserInteraction::getEventId)
                .toList();

        Set<Long> interactedEvents = interactionsByEvent.keySet();
        Map<Long, Double> candidates = new HashMap<>();

        for (EventSimilarity similarity : similarityRepository.findForEvents(recentEvents)) {
            if (recentEvents.contains(similarity.getEventA())
                    && !interactedEvents.contains(similarity.getEventB())) {
                candidates.merge(similarity.getEventB(), similarity.getScore(), Math::max);
            }
            if (recentEvents.contains(similarity.getEventB())
                    && !interactedEvents.contains(similarity.getEventA())) {
                candidates.merge(similarity.getEventA(), similarity.getScore(), Math::max);
            }
        }

        return candidates.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(entry -> toRecommendation(
                        entry.getKey(),
                        predictScore(entry.getKey(), interactionsByEvent)
                ))
                .sorted(Comparator.comparingDouble(RecommendedEventProto::getScore).reversed())
                .toList();
    }

    public List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        if (maxResults <= 0) {
            return List.of();
        }

        Set<Long> interactedEvents = interactionRepository
                .findByUserId(userId)
                .stream()
                .map(UserInteraction::getEventId)
                .collect(Collectors.toSet());

        return similarityRepository.findForEvent(eventId).stream()
                .map(similarity -> {
                    long candidate = similarity.getEventA() == eventId
                            ? similarity.getEventB()
                            : similarity.getEventA();
                    return Map.entry(candidate, similarity.getScore());
                })
                .filter(entry -> !interactedEvents.contains(entry.getKey()))
                .limit(maxResults)
                .map(entry -> toRecommendation(entry.getKey(), entry.getValue()))
                .toList();
    }

    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Double> counts = interactionRepository
                .sumWeightsByEventIds(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).doubleValue()
                ));

        return eventIds.stream()
                .distinct()
                .map(eventId -> toRecommendation(
                        eventId,
                        counts.getOrDefault(eventId, 0.0)
                ))
                .toList();
    }

    private double predictScore(long candidateEventId,
                                Map<Long, UserInteraction> interactionsByEvent) {
        List<Map.Entry<Double, Double>> neighbours =
                similarityRepository.findForEvent(candidateEventId).stream()
                        .map(similarity -> {
                            long otherId = similarity.getEventA() == candidateEventId
                                    ? similarity.getEventB()
                                    : similarity.getEventA();
                            UserInteraction interaction = interactionsByEvent.get(otherId);
                            if (interaction == null) {
                                return null;
                            }
                            return Map.entry(similarity.getScore(), interaction.getWeight());
                        })
                        .filter(Objects::nonNull)
                        .sorted(Map.Entry.<Double, Double>comparingByKey().reversed())
                        .limit(NEIGHBOURS_LIMIT)
                        .toList();

        double weightedSum = neighbours.stream()
                .mapToDouble(entry -> entry.getKey() * entry.getValue())
                .sum();
        double similaritiesSum = neighbours.stream()
                .mapToDouble(Map.Entry::getKey)
                .sum();

        return similaritiesSum == 0.0 ? 0.0 : weightedSum / similaritiesSum;
    }

    private RecommendedEventProto toRecommendation(long eventId, double score) {
        return RecommendedEventProto.newBuilder()
                .setEventId(eventId)
                .setScore(score)
                .build();
    }
}
