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

        Set<Long> recentEvents = allInteractions.stream()
                .sorted(Comparator.comparing(UserInteraction::getUpdatedAt).reversed())
                .limit(RECENT_INTERACTIONS_LIMIT)
                .map(UserInteraction::getEventId)
                .collect(Collectors.toSet());

        Set<Long> interactedEvents = interactionsByEvent.keySet();
        Map<Long, Double> candidates = new HashMap<>();

        for (EventSimilarity similarity : similarityRepository.findForEvents(List.copyOf(recentEvents))) {
            if (recentEvents.contains(similarity.getEventA())
                    && !interactedEvents.contains(similarity.getEventB())) {
                candidates.merge(similarity.getEventB(), similarity.getScore(), Math::max);
            }
            if (recentEvents.contains(similarity.getEventB())
                    && !interactedEvents.contains(similarity.getEventA())) {
                candidates.merge(similarity.getEventA(), similarity.getScore(), Math::max);
            }
        }

        List<Long> selectedCandidates = candidates.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(Map.Entry::getKey)
                .toList();
        if (selectedCandidates.isEmpty()) {
            return List.of();
        }

        // Load all candidate similarities once instead of querying per candidate (N+1).
        Set<Long> selectedIds = new HashSet<>(selectedCandidates);
        Map<Long, List<Map.Entry<Double, Double>>> neighboursByCandidate = new HashMap<>();
        for (EventSimilarity similarity : similarityRepository.findForEvents(selectedCandidates)) {
            if (selectedIds.contains(similarity.getEventA())) {
                UserInteraction interaction = interactionsByEvent.get(similarity.getEventB());
                if (interaction != null) {
                    neighboursByCandidate.computeIfAbsent(similarity.getEventA(), ignored -> new ArrayList<>())
                            .add(Map.entry(similarity.getScore(), interaction.getWeight()));
                }
            }
            if (selectedIds.contains(similarity.getEventB())) {
                UserInteraction interaction = interactionsByEvent.get(similarity.getEventA());
                if (interaction != null) {
                    neighboursByCandidate.computeIfAbsent(similarity.getEventB(), ignored -> new ArrayList<>())
                            .add(Map.entry(similarity.getScore(), interaction.getWeight()));
                }
            }
        }

        return selectedCandidates.stream()
                .map(eventId -> toRecommendation(eventId,
                        predictScore(neighboursByCandidate.getOrDefault(eventId, List.of()))))
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

    private double predictScore(List<Map.Entry<Double, Double>> similarities) {
        List<Map.Entry<Double, Double>> neighbours = similarities.stream()
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
