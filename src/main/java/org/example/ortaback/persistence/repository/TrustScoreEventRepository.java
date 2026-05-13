package org.example.ortaback.persistence.repository;

import org.example.ortaback.persistence.models.TrustScoreEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrustScoreEventRepository extends JpaRepository<TrustScoreEvent, Long> {

    /** История изменений пользователя, новые сверху, ограничено {@code limit}. */
    @Query("""
            SELECT t FROM TrustScoreEvent t
             WHERE t.user.id = :userId
             ORDER BY t.createdAt DESC
            """)
    List<TrustScoreEvent> findByUserIdOrderedDesc(@Param("userId") Long userId,
                                                  org.springframework.data.domain.Pageable pageable);

    /** Сумма дельт за последние {@code since} — для виджета «Last 30 days». */
    @Query("""
            SELECT COALESCE(SUM(t.delta), 0)
              FROM TrustScoreEvent t
             WHERE t.user.id   = :userId
               AND t.createdAt >= :since
            """)
    Integer sumDeltaSince(@Param("userId") Long userId,
                          @Param("since") LocalDateTime since);
}
