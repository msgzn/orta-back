package org.example.ortaback.mapper;

import org.example.ortaback.dto.response.CategoryDto;
import org.example.ortaback.dto.response.CategoryTreeNodeDto;
import org.example.ortaback.persistence.models.Category;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryMapperTest {

    private final CategoryMapper mapper = new CategoryMapper();

    // -----------------------------------------------------------------------
    //  toDto
    // -----------------------------------------------------------------------

    @Test
    void toDto_rootCategory_parentIdIsNull() {
        Category sport = Category.builder()
                .id(1L).name("Sport").iconUrl("https://cdn/sport.svg")
                .build();

        CategoryDto dto = mapper.toDto(sport);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.parentId()).isNull();
        assertThat(dto.name()).isEqualTo("Sport");
        assertThat(dto.iconUrl()).isEqualTo("https://cdn/sport.svg");
    }

    @Test
    void toDto_childCategory_carriesParentId() {
        Category parent = Category.builder().id(1L).name("Sport").build();
        Category child  = Category.builder().id(2L).name("Basketball").parent(parent).build();

        CategoryDto dto = mapper.toDto(child);

        assertThat(dto.parentId()).isEqualTo(1L);
    }

    // -----------------------------------------------------------------------
    //  toTree
    // -----------------------------------------------------------------------

    @Test
    void toTree_emptyInput_returnsEmptyList() {
        assertThat(mapper.toTree(List.of())).isEmpty();
    }

    @Test
    void toTree_attachesChildrenUnderParents() {
        Category sport = Category.builder().id(1L).name("Sport").build();
        Category basketball = Category.builder().id(2L).name("Basketball").parent(sport).build();
        Category football   = Category.builder().id(3L).name("Football").parent(sport).build();

        List<CategoryTreeNodeDto> tree = mapper.toTree(List.of(sport, basketball, football));

        assertThat(tree).hasSize(1);
        CategoryTreeNodeDto root = tree.get(0);
        assertThat(root.id()).isEqualTo(1L);
        assertThat(root.children()).hasSize(2);
        // Дети отсортированы по имени: Basketball < Football
        assertThat(root.children())
                .extracting(CategoryTreeNodeDto::name)
                .containsExactly("Basketball", "Football");
    }

    @Test
    void toTree_supportsThreeLevelsDeep() {
        Category sport      = Category.builder().id(1L).name("Sport").build();
        Category teamSports = Category.builder().id(2L).name("Team").parent(sport).build();
        Category basketball = Category.builder().id(3L).name("Basketball").parent(teamSports).build();

        List<CategoryTreeNodeDto> tree = mapper.toTree(List.of(sport, teamSports, basketball));

        assertThat(tree).hasSize(1);
        CategoryTreeNodeDto level1 = tree.get(0);
        assertThat(level1.name()).isEqualTo("Sport");

        CategoryTreeNodeDto level2 = level1.children().get(0);
        assertThat(level2.name()).isEqualTo("Team");

        CategoryTreeNodeDto level3 = level2.children().get(0);
        assertThat(level3.name()).isEqualTo("Basketball");
        assertThat(level3.children()).isNull();   // leaf — без children
    }

    @Test
    void toTree_orphanedNodeBecomesRoot() {
        // Категория ссылается на parent, которого нет в списке — должна
        // оказаться в roots (защита от dangling FK / частичной выборки).
        Category ghostParent = Category.builder().id(999L).name("ghost").build();
        Category orphan = Category.builder().id(1L).name("Orphan").parent(ghostParent).build();

        List<CategoryTreeNodeDto> tree = mapper.toTree(List.of(orphan));

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).name()).isEqualTo("Orphan");
    }

    @Test
    void toTree_rootsSortedByName() {
        Category z = Category.builder().id(1L).name("Z-Sport").build();
        Category a = Category.builder().id(2L).name("A-Music").build();

        List<CategoryTreeNodeDto> tree = mapper.toTree(List.of(z, a));

        assertThat(tree)
                .extracting(CategoryTreeNodeDto::name)
                .containsExactly("A-Music", "Z-Sport");
    }
}
