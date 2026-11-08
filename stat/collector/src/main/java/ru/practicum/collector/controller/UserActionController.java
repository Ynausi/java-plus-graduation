package ru.practicum.collector.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.kafka.common.KafkaException;
import ru.practicum.collector.kafka.UserActionPublisher;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class UserActionController extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final UserActionPublisher publisher;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            if (request.getUserId() <= 0 || request.getEventId() <= 0 || !request.hasTimestamp()) {
                throw new IllegalArgumentException("User id, event id and timestamp must be provided");
            }

            Instant timestamp = Instant.ofEpochSecond(
                    request.getTimestamp().getSeconds(),
                    request.getTimestamp().getNanos()
            );

            UserActionAvro action = UserActionAvro.newBuilder()
                    .setUserId(request.getUserId())
                    .setEventId(request.getEventId())
                    .setActionType(toAvro(request.getActionType()))
                    .setTimestamp(timestamp)
                    .build();

            publisher.publish(action);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (KafkaException e) {
            log.error("Unable to publish user action", e);
            responseObserver.onError(Status.UNAVAILABLE
                    .withDescription("Kafka is unavailable")
                    .withCause(e).asRuntimeException());
        } catch (RuntimeException e) {
            log.error("Collector gRPC request failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Unable to process user action")
                    .withCause(e).asRuntimeException());
        }
    }

    private ActionTypeAvro toAvro(ActionTypeProto actionType) {
        return switch (actionType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown action type");
        };
    }
}
