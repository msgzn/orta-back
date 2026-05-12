package org.example.ortaback.mapper;

import org.example.ortaback.dto.response.ParticipantResponseDto;
import org.example.ortaback.persistence.models.EventParticipant;
import org.example.ortaback.persistence.models.User;
import org.springframework.stereotype.Component;

/**
 * {@code EventParticipant + User} → {@link ParticipantResponseDto}.
 *
 * <p>Триггерит lazy-load на {@code participant.getUser()} — для списка
 * участников одного события это допустимо (1 SQL на пользователя).
 * При необходимости можно поджарить через JOIN FETCH.
 */
@Component
public class ParticipantMapper {

    public ParticipantResponseDto toDto(EventParticipant participant) {
        User user = participant.getUser();
        return new ParticipantResponseDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getAvatarUrl(),
                participant.getStatus(),
                participant.getJoinedAt(),
                participant.getAttendAt()
        );
    }
}
