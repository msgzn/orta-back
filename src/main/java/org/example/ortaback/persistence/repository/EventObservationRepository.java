package org.example.ortaback.persistence.repository;

import org.example.ortaback.persistence.models.EventObservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventObservationRepository extends JpaRepository<EventObservation, Long> {

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    void deleteByEventIdAndUserId(Long eventId, Long userId);

    Page<EventObservation> findByUserId(Long userId, Pageable pageable);

    /**
     * id пользователей, наблюдающих за указанным событием — для push-fan-out
     * при достижении {@code minParticipants}.
     */
    @Query("SELECT o.user.id FROM EventObservation o WHERE o.event.id = :eventId")
    List<Long> findObserverIdsByEventId(@Param("eventId") Long eventId);
}
