package org.example.ortaback.controller;

import org.example.ortaback.dto.response.UniversityDto;
import org.example.ortaback.exception.ResourceNotFoundException;
import org.example.ortaback.service.UniversityService;
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

class UniversityControllerTest extends MockMvcTestSupport {

    private final UniversityService universityService = mock(UniversityService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = buildMockMvc(new UniversityController(universityService));
    }

    @Test
    void list_returnsAllUniversities() throws Exception {
        when(universityService.listAll()).thenReturn(List.of(
                new UniversityDto(1L, "KazNU", "@kaznu.kz", List.of("@kaznu.kz"), "Almaty"),
                new UniversityDto(2L, "AITU", "@aitu.kz", List.of("@aitu.kz"), "Astana")
        ));

        mvc.perform(get("/api/v1/universities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("KazNU"))
                .andExpect(jsonPath("$[0].domainPattern").value("@kaznu.kz"))
                .andExpect(jsonPath("$[1].name").value("AITU"));
    }

    @Test
    void getById_returnsSingle() throws Exception {
        when(universityService.getById(1L))
                .thenReturn(new UniversityDto(1L, "KazNU", "@kaznu.kz", List.of("@kaznu.kz"), "Almaty"));

        mvc.perform(get("/api/v1/universities/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.address").value("Almaty"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(universityService.getById(999L))
                .thenThrow(new ResourceNotFoundException("University not found: id=999"));

        mvc.perform(get("/api/v1/universities/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("University not found: id=999"));
    }
}
