package org.example.ortaback.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.example.ortaback.persistence.models.enums.ParticipantStatus;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ParticipantResponseDto(

        Long userId,
        String firstName,
        String lastName,
        String avatarUrl,
        ParticipantStatus status,
        LocalDateTime joinedAt,
        Boolean attended
) {}
