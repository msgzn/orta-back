package org.example.ortaback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.ortaback.dto.response.CategoryDto;
import org.example.ortaback.dto.response.CategoryTreeNodeDto;
import org.example.ortaback.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Публичный справочник категорий — открыт без авторизации
 * (см. {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Плоский список всех категорий")
    @GetMapping
    public List<CategoryDto> list() {
        return categoryService.listAll();
    }

    @Operation(summary = "Иерархическое дерево категорий")
    @GetMapping("/tree")
    public List<CategoryTreeNodeDto> tree() {
        return categoryService.listTree();
    }
}
