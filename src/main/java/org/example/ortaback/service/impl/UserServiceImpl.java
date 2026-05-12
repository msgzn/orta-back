package org.example.ortaback.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.ortaback.exception.ResourceNotFoundException;
import org.example.ortaback.geo.CoordinateValidator;
import org.example.ortaback.persistence.repository.UserRepository;
import org.example.ortaback.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CoordinateValidator coordinateValidator;

    @Override
    @Transactional
    public void updateLastKnownLocation(Long userId, double lat, double lng) {
        coordinateValidator.validateLatLng(lat, lng);

        int updated = userRepository.updateLastKnownLocation(userId, lat, lng, LocalDateTime.now());
        if (updated == 0) {
            throw new ResourceNotFoundException("User not found: id=" + userId);
        }
    }
}
