package org.example.ortaback.service;

import org.example.ortaback.dto.response.UniversityDto;

import java.util.List;

public interface UniversityService {

    List<UniversityDto> listAll();

    UniversityDto getById(Long id);
}
