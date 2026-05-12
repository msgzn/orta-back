package org.example.ortaback.mapper;

import org.example.ortaback.dto.response.UniversityDto;
import org.example.ortaback.persistence.models.University;
import org.springframework.stereotype.Component;

@Component
public class UniversityMapper {

    public UniversityDto toDto(University university) {
        return new UniversityDto(
                university.getId(),
                university.getName(),
                university.getDomainPattern(),
                university.getAddress()
        );
    }
}
