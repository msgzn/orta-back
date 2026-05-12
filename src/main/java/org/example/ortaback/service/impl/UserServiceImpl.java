package org.example.ortaback.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.ortaback.dto.request.UserProfileUpdateRequestDto;
import org.example.ortaback.dto.response.CategoryDto;
import org.example.ortaback.dto.response.UserProfileDto;
import org.example.ortaback.exception.ResourceNotFoundException;
import org.example.ortaback.geo.CoordinateValidator;
import org.example.ortaback.mapper.CategoryMapper;
import org.example.ortaback.mapper.UserMapper;
import org.example.ortaback.persistence.models.Category;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.UserInterest;
import org.example.ortaback.persistence.repository.CategoryRepository;
import org.example.ortaback.persistence.repository.UserInterestRepository;
import org.example.ortaback.persistence.repository.UserRepository;
import org.example.ortaback.service.UserService;
import org.example.ortaback.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String AVATARS_FOLDER = "avatars";

    private final UserRepository userRepository;
    private final UserInterestRepository userInterestRepository;
    private final CategoryRepository categoryRepository;
    private final CoordinateValidator coordinateValidator;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final StorageService storageService;

    @Override
    @Transactional
    public void updateLastKnownLocation(Long userId, double lat, double lng) {
        coordinateValidator.validateLatLng(lat, lng);

        int updated = userRepository.updateLastKnownLocation(userId, lat, lng, LocalDateTime.now());
        if (updated == 0) {
            throw new ResourceNotFoundException("User not found: id=" + userId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getProfile(Long userId) {
        User user = loadUser(userId);
        List<UserInterest> interests = userInterestRepository.findByUserId(userId);
        return userMapper.toProfile(user, interests);
    }

    @Override
    @Transactional
    public UserProfileDto updateProfile(Long userId, UserProfileUpdateRequestDto request) {
        User user = loadUser(userId);

        if (request.firstName()      != null) user.setFirstName(request.firstName());
        if (request.lastName()       != null) user.setLastName(request.lastName());
        if (request.phoneNumber()    != null) user.setPhoneNumber(request.phoneNumber().isBlank()
                ? null
                : request.phoneNumber());
        if (request.searchRadiusKm() != null) user.setSearchRadiusKm(request.searchRadiusKm());

        // dirty-checking — изменения улетят на commit.

        List<UserInterest> interests = userInterestRepository.findByUserId(userId);
        return userMapper.toProfile(user, interests);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> listInterests(Long userId) {
        return userInterestRepository.findByUserId(userId).stream()
                .map(UserInterest::getCategory)
                .map(categoryMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void addInterest(Long userId, Long categoryId) {
        if (userInterestRepository.existsByUserIdAndCategoryId(userId, categoryId)) {
            // идемпотентно
            return;
        }
        User user = loadUser(userId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: id=" + categoryId));

        userInterestRepository.save(UserInterest.builder()
                .user(user)
                .category(category)
                .build());
    }

    @Override
    @Transactional
    public void removeInterest(Long userId, Long categoryId) {
        userInterestRepository.deleteByUserIdAndCategoryId(userId, categoryId);
    }

    @Override
    @Transactional
    public String uploadAvatar(Long userId, MultipartFile file) {
        User user = loadUser(userId);

        // Загружаем новый, потом удаляем старый — если новый загрузился неудачно,
        // у пользователя остаётся работающий старый аватар.
        StorageService.UploadedFile uploaded = storageService.upload(file, AVATARS_FOLDER);

        String previousUrl = user.getAvatarUrl();
        user.setAvatarUrl(uploaded.url());

        if (previousUrl != null && !previousUrl.equals(uploaded.url())) {
            storageService.deleteByPublicUrl(previousUrl);
        }
        return uploaded.url();
    }

    @Override
    @Transactional
    public void deleteAvatar(Long userId) {
        User user = loadUser(userId);
        String url = user.getAvatarUrl();
        if (url == null) {
            return;
        }
        user.setAvatarUrl(null);
        storageService.deleteByPublicUrl(url);
    }

    // -----------------------------------------------------------------------

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: id=" + userId));
    }
}
