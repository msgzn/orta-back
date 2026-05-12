package org.example.ortaback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.ortaback.dto.response.UniversityDto;
import org.example.ortaback.service.UniversityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Публичный справочник университетов. Используется фронтом для:
 * <ul>
 *   <li>подсветки «вы регистрируетесь как студент X» во время ввода email
 *       (домен сматчился);</li>
 *   <li>выпадающего списка на форме регистрации, если автодетект не сработал.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/universities")
@RequiredArgsConstructor
@Tag(name = "Universities")
public class UniversityController {

    private final UniversityService universityService;

    @Operation(summary = "Список всех университетов")
    @GetMapping
    public List<UniversityDto> list() {
        return universityService.listAll();
    }

    @Operation(summary = "Карточка университета")
    @GetMapping("/{id}")
    public UniversityDto get(@PathVariable Long id) {
        return universityService.getById(id);
    }
}
