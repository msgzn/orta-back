package org.example.ortaback.persistence.repository;

import org.example.ortaback.persistence.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Корневые категории (без родителя). */
    List<Category> findByParentIsNull();

    List<Category> findByParentId(Long parentId);
}
