package org.example.ortaback.mapper;

import org.example.ortaback.dto.response.UniversityDto;
import org.example.ortaback.persistence.models.University;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UniversityMapper {

    public UniversityDto toDto(University university) {
        List<String> domains = university.getDomains() == null
                ? List.of()
                : university.getDomains().stream().sorted().toList();
        return new UniversityDto(
                university.getId(),
                university.getName(),
                university.getDomainPattern(),
                domains,
                university.getAddress()
        );
    }
}
