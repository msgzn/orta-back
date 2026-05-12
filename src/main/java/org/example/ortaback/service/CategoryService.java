package org.example.ortaback.service;

import org.example.ortaback.dto.response.CategoryDto;
import org.example.ortaback.dto.response.CategoryTreeNodeDto;

import java.util.List;

public interface CategoryService {

    /** Плоский список всех категорий, отсортированный по имени. */
    List<CategoryDto> listAll();

    /** Иерархическое дерево категорий: корни → дети → внуки. */
    List<CategoryTreeNodeDto> listTree();
}
