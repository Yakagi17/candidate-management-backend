package com.candidatemanagement.controller;

import com.candidatemanagement.dto.VacancyRequestDto;
import com.candidatemanagement.exception.GenericApiException;
import com.candidatemanagement.model.Vacancy;
import com.candidatemanagement.model.criteria.Criterion;
import com.candidatemanagement.service.VacancyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VacancyController.class)
class VacancyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VacancyService vacancyService;

    @Autowired
    private ObjectMapper objectMapper;

    private VacancyRequestDto vacancyRequestDto;
    private Vacancy testVacancy;
    private Set<Criterion> testCriteria;

    @BeforeEach
    void setUp() {
        // Create test criteria
        testCriteria = new HashSet<>();

        Criterion salaryRangeCriterion = new Criterion(
            "currentSalary",
            5,
            Criterion.CriterionDetails.createObject("RANGE", new BigDecimal("5000000"), new BigDecimal("10000000"), null)
        );

        Criterion ageCriterion = new Criterion(
            "age",
            3,
            Criterion.CriterionDetails.createObject("RANGE", new BigDecimal("25"), new BigDecimal("40"), null)
        );

        testCriteria.add(salaryRangeCriterion);
        testCriteria.add(ageCriterion);

        // Create test DTO and entity
        vacancyRequestDto = new VacancyRequestDto("Software Engineer", testCriteria);

        testVacancy = new Vacancy("Software Engineer", testCriteria);
        testVacancy.setId("1");
    }

    @Test
    void createVacancy_Success() throws Exception {
        when(vacancyService.createVacancy(any(VacancyRequestDto.class))).thenReturn(testVacancy);

        mockMvc.perform(post("/api/v1/vacancies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(vacancyRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("1")))
                .andExpect(jsonPath("$.name", is("Software Engineer")))
                .andExpect(jsonPath("$.criteria", hasSize(2)));

        verify(vacancyService).createVacancy(any(VacancyRequestDto.class));
    }

    @Test
    void createVacancy_ServiceThrowsException() throws Exception {
        when(vacancyService.createVacancy(any(VacancyRequestDto.class)))
                .thenThrow(new GenericApiException(HttpStatus.BAD_REQUEST, "Validation Error", "Invalid data"));

        mockMvc.perform(post("/api/v1/vacancies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(vacancyRequestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Validation Error")))
                .andExpect(jsonPath("$.message", is("Invalid data")));

        verify(vacancyService).createVacancy(any(VacancyRequestDto.class));
    }

    @Test
    void getAllVacancies_Success() throws Exception {
        when(vacancyService.getAllVacancies()).thenReturn(Collections.singletonList(testVacancy));

        mockMvc.perform(get("/api/v1/vacancies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("1")))
                .andExpect(jsonPath("$[0].name", is("Software Engineer")));

        verify(vacancyService).getAllVacancies();
    }

    @Test
    void getAllVacancies_EmptyList() throws Exception {
        when(vacancyService.getAllVacancies()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/vacancies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(vacancyService).getAllVacancies();
    }

    @Test
    void getVacancyById_Success() throws Exception {
        when(vacancyService.getVacancyById("1")).thenReturn(Optional.of(testVacancy));

        mockMvc.perform(get("/api/v1/vacancies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("1")))
                .andExpect(jsonPath("$.name", is("Software Engineer")));

        verify(vacancyService).getVacancyById("1");
    }

    @Test
    void getVacancyById_NotFound() throws Exception {
        when(vacancyService.getVacancyById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/vacancies/nonexistent"))
                .andExpect(status().isNotFound());

        verify(vacancyService).getVacancyById("nonexistent");
    }

    @Test
    void updateVacancy_Success() throws Exception {
        // Create updated vacancy
        Vacancy updatedVacancy = new Vacancy("Senior Software Engineer", testCriteria);
        updatedVacancy.setId("1");

        when(vacancyService.updateVacancy(eq("1"), any(VacancyRequestDto.class))).thenReturn(updatedVacancy);

        VacancyRequestDto updateRequest = new VacancyRequestDto("Senior Software Engineer", null);

        mockMvc.perform(patch("/api/v1/vacancies/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("1")))
                .andExpect(jsonPath("$.name", is("Senior Software Engineer")));

        verify(vacancyService).updateVacancy(eq("1"), any(VacancyRequestDto.class));
    }

    @Test
    void updateVacancy_NotFound() throws Exception {
        when(vacancyService.updateVacancy(eq("nonexistent"), any(VacancyRequestDto.class)))
                .thenThrow(new GenericApiException(HttpStatus.NOT_FOUND, "Vacancy Not Found", "Vacancy not found"));

        VacancyRequestDto updateRequest = new VacancyRequestDto("Updated Name", null);

        mockMvc.perform(patch("/api/v1/vacancies/nonexistent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Vacancy Not Found")));

        verify(vacancyService).updateVacancy(eq("nonexistent"), any(VacancyRequestDto.class));
    }

    @Test
    void updateVacancy_ValidationError() throws Exception {
        when(vacancyService.updateVacancy(eq("1"), any(VacancyRequestDto.class)))
                .thenThrow(new GenericApiException(HttpStatus.BAD_REQUEST, "Validation Error", "Invalid data"));

        VacancyRequestDto updateRequest = new VacancyRequestDto("", null);

        mockMvc.perform(patch("/api/v1/vacancies/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Validation Error")));

        verify(vacancyService).updateVacancy(eq("1"), any(VacancyRequestDto.class));
    }

    @Test
    void deleteVacancy_Success() throws Exception {
        doNothing().when(vacancyService).deleteVacancy("1");

        mockMvc.perform(delete("/api/v1/vacancies/1"))
                .andExpect(status().isNoContent());

        verify(vacancyService).deleteVacancy("1");
    }

    @Test
    void deleteVacancy_NotFound() throws Exception {
        doThrow(new GenericApiException(HttpStatus.NOT_FOUND, "Vacancy Not Found", "Vacancy not found"))
                .when(vacancyService).deleteVacancy("nonexistent");

        mockMvc.perform(delete("/api/v1/vacancies/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Vacancy Not Found")));

        verify(vacancyService).deleteVacancy("nonexistent");
    }
}

