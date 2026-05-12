package org.example.ortaback.persistence.repository;

import org.example.ortaback.persistence.models.EventParticipant;
import org.example.ortaback.persistence.models.enums.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {

    List<EventParticipant> findByEventId(Long eventId);

    List<EventParticipant> findByUserId(Long userId);

    Optional<EventParticipant> findByEventIdAndUserId(Long eventId, Long userId);

    boolean existsByEventIdAndUserIdAndStatus(Long eventId, Long userId, ParticipantStatus status);

    long countByEventIdAndStatus(Long eventId, ParticipantStatus status);
}
