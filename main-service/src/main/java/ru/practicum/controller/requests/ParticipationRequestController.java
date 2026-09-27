package ru.practicum.controller.requests;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.requests.ParticipationRequestDto;
import ru.practicum.service.requests.ParticipationRequestService;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ParticipationRequestController {

    private final ParticipationRequestService requestService;

    @PostMapping("/users/{userId}/requests")
    public ResponseEntity<ParticipationRequestDto> addParticipationRequest(
            @PathVariable Long userId,
            @RequestParam Long eventId) {
        ParticipationRequestDto newRequest = requestService.addParticipationRequest(userId, eventId);

        return ResponseEntity.status(HttpStatus.CREATED).body(newRequest);
    }

    @PatchMapping("/users/{userId}/requests/{requestId}/cancel")
    public ResponseEntity<ParticipationRequestDto> cancelParticipationRequest(
            @PathVariable Long userId,
            @PathVariable Long requestId) {
        return ResponseEntity.ok(requestService.cancelParticipationRequest(userId, requestId));
    }

    @GetMapping("/users/{userId}/requests")
    public ResponseEntity<List<ParticipationRequestDto>> getCurrentUserRequests(
            @PathVariable Long userId) {
        return ResponseEntity.ok(requestService.getCurrentUserRequests(userId));
    }
}
