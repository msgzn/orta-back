package org.example.ortaback.controller;

import org.example.ortaback.dto.response.CategoryDto;
import org.example.ortaback.dto.response.CategoryTreeNodeDto;
import org.example.ortaback.service.CategoryService;
import org.example.ortaback.testsupport.MockMvcTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryControllerTest extends MockMvcTestSupport {

    private final CategoryService categoryService = mock(CategoryService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = buildMockMvc(new CategoryController(categoryService));
    }

    @Test
    void listFlat_returnsOkAndDelegatesToService() throws Exception {
        when(categoryService.listAll()).thenReturn(List.of(
                new CategoryDto(1L, null, "Sport", null),
                new CategoryDto(2L, 1L, "Basketball", null)
        ));

        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Sport"))
                .andExpect(jsonPath("$[1].parentId").value(1));
    }

    @Test
    void tree_returnsOkWithNestedChildren() throws Exception {
        CategoryTreeNodeDto child = new CategoryTreeNodeDto(2L, 1L, "Basketball", null, null);
        CategoryTreeNodeDto root = new CategoryTreeNodeDto(1L, null, "Sport", null, List.of(child));
        when(categoryService.listTree()).thenReturn(List.of(root));

        mvc.perform(get("/api/v1/categories/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Sport"))
                .andExpect(jsonPath("$[0].children[0].name").value("Basketball"));
    }
}
