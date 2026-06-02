package org.example.ortaback.persistence.repository;

import org.example.ortaback.persistence.models.University;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UniversityRepository extends JpaRepository<University, Long> {

    Optional<University> findByName(String name);

    /**
     * Находит ВУЗы, чей домен совпадает с host'ом email (суффикс-матч):
     * {@code host = domain} ИЛИ {@code host} заканчивается на {@code .domain}.
     * Сортировка по длине домена убыв. — самый специфичный матч первым.
     * Используется при регистрации для авто-привязки студента к ВУЗу.
     */
    @Query("""
            SELECT u FROM University u
            JOIN u.domains d
            WHERE :host = d OR :host LIKE CONCAT('%.', d)
            ORDER BY LENGTH(d) DESC
            """)
    List<University> findAllByEmailHost(@Param("host") String host);
}
