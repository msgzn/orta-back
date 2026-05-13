package org.example.ortaback.service.impl;

import org.example.ortaback.dto.request.UserProfileUpdateRequestDto;
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
import org.example.ortaback.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock UserInterestRepository userInterestRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock CoordinateValidator coordinateValidator;
    @Mock UserMapper userMapper;
    @Mock CategoryMapper categoryMapper;
    @Mock StorageService storageService;

    UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                userRepository, userInterestRepository, categoryRepository,
                coordinateValidator, userMapper, categoryMapper, storageService);
    }

    // =======================================================================
    //  updateLastKnownLocation
    // =======================================================================

    @Test
    void updateLastKnownLocation_validatesCoordinatesBeforeDb() {
        when(userRepository.updateLastKnownLocation(eq(1L), anyDouble(), anyDouble(), any()))
                .thenReturn(1);

        userService.updateLastKnownLocation(1L, 43.22, 76.85);

        InOrder order = inOrder(coordinateValidator, userRepository);
        order.verify(coordinateValidator).validateLatLng(43.22, 76.85);
        order.verify(userRepository).updateLastKnownLocation(eq(1L), eq(43.22), eq(76.85), any());
    }

    @Test
    void updateLastKnownLocation_userNotFound_throws() {
        when(userRepository.updateLastKnownLocation(anyLong(), anyDouble(), anyDouble(), any()))
                .thenReturn(0);

        assertThatThrownBy(() -> userService.updateLastKnownLocation(999L, 43.22, 76.85))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =======================================================================
    //  updateProfile
    // =======================================================================

    @Test
    void updateProfile_appliesOnlyNonNullFields() {
        User user = User.builder()
                .id(1L).firstName("Old").lastName("Name")
                .phoneNumber("+1").searchRadiusKm(20)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userInterestRepository.findByUserId(1L)).thenReturn(java.util.List.of());

        UserProfileUpdateRequestDto request = new UserProfileUpdateRequestDto(
                "New", null, null, null, null, null, null, null, null, null);

        userService.updateProfile(1L, request);

        assertThat(user.getFirstName()).isEqualTo("New");
        assertThat(user.getLastName()).isEqualTo("Name");     // не тронут
        assertThat(user.getPhoneNumber()).isEqualTo("+1");    // не тронут
        assertThat(user.getSearchRadiusKm()).isEqualTo(20);   // не тронут
    }

    @Test
    void updateProfile_blankPhone_becomesNull() {
        User user = User.builder().id(1L).phoneNumber("+1").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userInterestRepository.findByUserId(1L)).thenReturn(java.util.List.of());

        userService.updateProfile(1L, new UserProfileUpdateRequestDto(
                null, null, null, "   ", null, null, null, null, null, null));

        assertThat(user.getPhoneNumber()).isNull();
    }

    // =======================================================================
    //  addInterest / removeInterest — идемпотентность
    // =======================================================================

    @Test
    void addInterest_existingPair_noOp() {
        when(userInterestRepository.existsByUserIdAndCategoryId(1L, 10L)).thenReturn(true);

        userService.addInterest(1L, 10L);

        verify(userInterestRepository, never()).save(any());
    }

    @Test
    void addInterest_categoryNotFound_throws() {
        when(userInterestRepository.existsByUserIdAndCategoryId(1L, 999L)).thenReturn(false);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).build()));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.addInterest(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addInterest_newPair_persistsUserInterest() {
        User user = User.builder().id(1L).build();
        Category cat = Category.builder().id(10L).name("Football").build();

        when(userInterestRepository.existsByUserIdAndCategoryId(1L, 10L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(cat));

        userService.addInterest(1L, 10L);

        ArgumentCaptor<UserInterest> captor = ArgumentCaptor.forClass(UserInterest.class);
        verify(userInterestRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getCategory()).isEqualTo(cat);
    }

    // =======================================================================
    //  Аватар: upload / delete order
    // =======================================================================

    @Test
    void uploadAvatar_replacesOldUrlAndDeletesPrevious() {
        User user = User.builder().id(1L).avatarUrl("https://s3/old.jpg").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        MockMultipartFile file = new MockMultipartFile("file", "x.jpg", "image/jpeg", "data".getBytes());
        when(storageService.upload(eq(file), eq("avatars")))
                .thenReturn(new StorageService.UploadedFile("avatars/new.jpg", "https://s3/new.jpg"));

        String url = userService.uploadAvatar(1L, file);

        assertThat(url).isEqualTo("https://s3/new.jpg");
        assertThat(user.getAvatarUrl()).isEqualTo("https://s3/new.jpg");

        // Порядок: сначала upload, потом set, потом delete старого.
        InOrder order = inOrder(storageService);
        order.verify(storageService).upload(file, "avatars");
        order.verify(storageService).deleteByPublicUrl("https://s3/old.jpg");
    }

    @Test
    void uploadAvatar_noPreviousAvatar_doesNotCallDelete() {
        User user = User.builder().id(1L).avatarUrl(null).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        MockMultipartFile file = new MockMultipartFile("file", "x.jpg", "image/jpeg", "data".getBytes());
        when(storageService.upload(any(), any()))
                .thenReturn(new StorageService.UploadedFile("avatars/new.jpg", "https://s3/new.jpg"));

        userService.uploadAvatar(1L, file);

        verify(storageService, never()).deleteByPublicUrl(any());
    }

    @Test
    void deleteAvatar_noOpIfUserHasNone() {
        User user = User.builder().id(1L).avatarUrl(null).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteAvatar(1L);

        verify(storageService, never()).deleteByPublicUrl(any());
        assertThat(user.getAvatarUrl()).isNull();
    }

    @Test
    void deleteAvatar_clearsUrlAndDeletesObject() {
        User user = User.builder().id(1L).avatarUrl("https://s3/file.jpg").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteAvatar(1L);

        assertThat(user.getAvatarUrl()).isNull();
        verify(storageService).deleteByPublicUrl("https://s3/file.jpg");
    }
}
