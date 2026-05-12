package org.example.ortaback.persistence.repository;

import org.example.ortaback.persistence.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Используется Spring Security как поиск по «username» (= email). */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Точечное обновление геопозиции — пишем нативным {@code UPDATE},
     * чтобы не подтягивать всю сущность ради двух полей.
     */
    @Modifying
    @Query(value = """
            UPDATE users
               SET last_known_location = ST_SetSRID(ST_MakePoint(:lng, :lat), 4326),
                   location_updated_at = :updatedAt
             WHERE id = :userId
            """, nativeQuery = true)
    int updateLastKnownLocation(@Param("userId") Long userId,
                                @Param("lat") double lat,
                                @Param("lng") double lng,
                                @Param("updatedAt") LocalDateTime updatedAt);
}
