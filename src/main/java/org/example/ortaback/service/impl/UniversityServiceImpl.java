package org.example.ortaback.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.ortaback.dto.response.UniversityDto;
import org.example.ortaback.exception.ResourceNotFoundException;
import org.example.ortaback.mapper.UniversityMapper;
import org.example.ortaback.persistence.models.University;
import org.example.ortaback.persistence.repository.UniversityRepository;
import org.example.ortaback.service.UniversityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UniversityServiceImpl implements UniversityService {

    private final UniversityRepository universityRepository;
    private final UniversityMapper universityMapper;

    @Override
    @Transactional(readOnly = true)
    public List<UniversityDto> listAll() {
        return universityRepository.findAll().stream()
                .sorted(Comparator.comparing(University::getName))
                .map(universityMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UniversityDto getById(Long id) {
        return universityRepository.findById(id)
                .map(universityMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("University not found: id=" + id));
    }
}
