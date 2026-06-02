package org.example.ortaback.persistence.repository;

import org.example.ortaback.persistence.models.EventMessageRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Маркеры прочтения чата (ТЗ §4.1). Одна строка на пару (событие,
 * пользователь) — гарантируется уникальным ключом
 * {@code uq_event_message_read_event_user}.
 */
@Repository
public interface EventMessageReadRepository extends JpaRepository<EventMessageRead, Long> {

    Optional<EventMessageRead> findByEvent_IdAndUser_Id(Long eventId, Long userId);
}
