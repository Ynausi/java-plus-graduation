package ru.practicum.stats;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
public class AnalyzerClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerStub;

    public List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        return toList(analyzerStub.getRecommendationsForUser(request));
    }

    public List<RecommendedEventProto> getSimilarEvents(
            long eventId,
            long userId,
            int maxResults) {
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        return toList(analyzerStub.getSimilarEvents(request));
    }

    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(eventIds)
                .build();

        return toList(analyzerStub.getInteractionsCount(request));
    }

    private List<RecommendedEventProto> toList(Iterator<RecommendedEventProto> iterator) {
        List<RecommendedEventProto> result = new ArrayList<>();
        iterator.forEachRemaining(result::add);
        return result;
    }
}
