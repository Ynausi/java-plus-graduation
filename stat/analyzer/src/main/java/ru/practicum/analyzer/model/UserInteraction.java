package ru.practicum.analyzer.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "user_interactions", uniqueConstraints = @UniqueConstraint(
        name = "uq_user_event_interaction", columnNames = {"user_id", "event_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInteraction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "event_id", nullable = false)
    private Long eventId;
    @Column(nullable = false)
    private Double weight;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
