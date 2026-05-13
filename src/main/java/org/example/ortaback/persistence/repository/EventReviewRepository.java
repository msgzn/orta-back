package org.example.ortaback.persistence.repository;

import org.example.ortaback.persistence.models.EventReview;
import org.example.ortaback.persistence.models.enums.ReviewType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventReviewRepository extends JpaRepository<EventReview, Long> {

    /**
     * «Один отзыв на (event, reviewer, target, type)» — application-level guard
     * перед insert'ом. Для VENUE target = NULL, что в Postgres unique-индексе
     * вело бы себя как «NULL ≠ NULL» (дубли разрешены); тут проверяем явно.
     */
    @Query("""
            SELECT (COUNT(r) > 0)
              FROM EventReview r
             WHERE r.event.id    = :eventId
               AND r.reviewer.id = :reviewerId
               AND r.type        = :type
               AND (
                    (:targetId IS NULL AND r.target IS NULL)
                 OR (r.target.id = :targetId)
               )
            """)
    boolean existsByEventAndReviewerAndType(@Param("eventId") Long eventId,
                                            @Param("reviewerId") Long reviewerId,
                                            @Param("targetId") Long targetId,
                                            @Param("type") ReviewType type);
}
