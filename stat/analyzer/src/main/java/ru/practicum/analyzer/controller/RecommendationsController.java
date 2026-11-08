package ru.practicum.analyzer.controller;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.dao.DataAccessException;
import ru.practicum.analyzer.service.RecommendationService;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;

import java.util.List;
import java.util.function.Supplier;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class RecommendationsController
        extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(
            UserPredictionsRequestProto request,
            StreamObserver<RecommendedEventProto> responseObserver) {
        respond(responseObserver, () -> {
            validateUserId(request.getUserId());
            validateMaxResults(request.getMaxResults());
            return recommendationService.getRecommendationsForUser(
                    request.getUserId(), request.getMaxResults());
        });
    }

    @Override
    public void getSimilarEvents(
            SimilarEventsRequestProto request,
            StreamObserver<RecommendedEventProto> responseObserver) {
        respond(responseObserver, () -> {
            validateUserId(request.getUserId());
            if (request.getEventId() <= 0) {
                throw new IllegalArgumentException("Event id must be positive");
            }
            validateMaxResults(request.getMaxResults());
            return recommendationService.getSimilarEvents(
                    request.getEventId(), request.getUserId(), request.getMaxResults());
        });
    }

    @Override
    public void getInteractionsCount(
            InteractionsCountRequestProto request,
            StreamObserver<RecommendedEventProto> responseObserver) {
        respond(responseObserver, () -> {
            if (request.getEventIdList().stream().anyMatch(id -> id <= 0)) {
                throw new IllegalArgumentException("Event ids must be positive");
            }
            return recommendationService.getInteractionsCount(request.getEventIdList());
        });
    }

    private void respond(StreamObserver<RecommendedEventProto> observer,
                         Supplier<List<RecommendedEventProto>> action) {
        try {
            action.get().forEach(observer::onNext);
            observer.onCompleted();
        } catch (IllegalArgumentException e) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (DataAccessException e) {
            log.error("Recommendation database is unavailable", e);
            observer.onError(Status.UNAVAILABLE
                    .withDescription("Recommendation database is unavailable")
                    .withCause(e).asRuntimeException());
        } catch (RuntimeException e) {
            log.error("Recommendation gRPC request failed", e);
            observer.onError(Status.INTERNAL
                    .withDescription("Unable to generate recommendations")
                    .withCause(e).asRuntimeException());
        }
    }

    private void validateUserId(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("User id must be positive");
        }
    }

    private void validateMaxResults(int maxResults) {
        if (maxResults < 0) {
            throw new IllegalArgumentException("Max results must not be negative");
        }
    }
}
