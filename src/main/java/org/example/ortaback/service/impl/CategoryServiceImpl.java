package org.example.ortaback.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.ortaback.dto.response.CategoryDto;
import org.example.ortaback.dto.response.CategoryTreeNodeDto;
import org.example.ortaback.mapper.CategoryMapper;
import org.example.ortaback.persistence.models.Category;
import org.example.ortaback.persistence.repository.CategoryRepository;
import org.example.ortaback.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> listAll() {
        return categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(Category::getName))
                .map(categoryMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryTreeNodeDto> listTree() {
        return categoryMapper.toTree(categoryRepository.findAll());
    }
}
