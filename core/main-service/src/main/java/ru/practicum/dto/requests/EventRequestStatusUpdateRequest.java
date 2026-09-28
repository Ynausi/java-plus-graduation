package ru.practicum.dto.requests;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.practicum.model.RequestStatus;

import java.util.List;

@Data
public class EventRequestStatusUpdateRequest {

    @NotEmpty(message = "Список идентификаторов заявок не может быть пустым")
    private List<Long> requestIds;

    @NotNull(message = "Статус заявки не может быть пустым")
    private RequestStatus status;
}