package org.example.ortaback.persistence.repository;

import org.example.ortaback.persistence.models.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {

    List<UserInterest> findByUserId(Long userId);

    boolean existsByUserIdAndCategoryId(Long userId, Long categoryId);

    void deleteByUserIdAndCategoryId(Long userId, Long categoryId);
}
