package org.example.ortaback.mapper;

import lombok.RequiredArgsConstructor;
import org.example.ortaback.dto.response.CategoryDto;
import org.example.ortaback.dto.response.NotificationPreferencesDto;
import org.example.ortaback.dto.response.UniversityMiniDto;
import org.example.ortaback.dto.response.UserProfileDto;
import org.example.ortaback.persistence.models.University;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.UserInterest;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final CategoryMapper categoryMapper;

    public UserProfileDto toProfile(User user, Collection<UserInterest> interests) {
        UniversityMiniDto universityMini = toUniversityMini(user.getUniversity());

        List<CategoryDto> interestDtos = interests.stream()
                .map(UserInterest::getCategory)
                .map(categoryMapper::toDto)
                .toList();

        NotificationPreferencesDto notifications = new NotificationPreferencesDto(
                user.getNotifyEventStart(),
                user.getNotifyCapacityReached(),
                user.getNotifyEventPublished(),
                user.getNotifyReviewReceived()
        );

        return new UserProfileDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getNickname(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getTrustScore(),
                user.getSearchRadiusKm(),
                user.getLanguage(),
                universityMini,
                user.getCreatedAt(),
                interestDtos,
                notifications
        );
    }

    private UniversityMiniDto toUniversityMini(University university) {
        if (university == null) {
            return null;
        }
        return new UniversityMiniDto(university.getId(), university.getName());
    }
}
