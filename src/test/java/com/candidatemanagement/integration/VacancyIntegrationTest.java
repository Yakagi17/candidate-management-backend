package com.candidatemanagement.integration;

import com.candidatemanagement.dto.VacancyRequestDto;
import com.candidatemanagement.model.Vacancy;
import com.candidatemanagement.model.criteria.Criterion;
import com.candidatemanagement.repository.VacancyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@AutoConfigureWebMvc
class VacancyIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.4.2");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private VacancyRepository vacancyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        vacancyRepository.deleteAll();
    }

    @Test
    void testVacancyLifecycle() throws Exception {
        // Step 1: Create a vacancy
        VacancyRequestDto juniorDevDto = createJuniorDeveloperVacancyDto();
        String vacancyJson = objectMapper.writeValueAsString(juniorDevDto);

        String response = mockMvc.perform(post("/api/v1/vacancies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(vacancyJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Junior Developer")))
                .andExpect(jsonPath("$.criteria", hasSize(3)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Vacancy createdVacancy = objectMapper.readValue(response, Vacancy.class);
        String vacancyId = createdVacancy.getId();

        // Step 2: Get the vacancy by ID
        mockMvc.perform(get("/api/v1/vacancies/" + vacancyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(vacancyId)))
                .andExpect(jsonPath("$.name", is("Junior Developer")));

        // Step 3: Get all vacancies
        mockMvc.perform(get("/api/v1/vacancies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(vacancyId)));

        // Step 4: Update the vacancy
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("name", "Junior Software Developer");
        String updateJson = objectMapper.writeValueAsString(updateRequest);

        mockMvc.perform(patch("/api/v1/vacancies/" + vacancyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Junior Software Developer")))
                .andExpect(jsonPath("$.criteria", hasSize(3))); // criteria should be unchanged

        // Step 5: Delete the vacancy
        mockMvc.perform(delete("/api/v1/vacancies/" + vacancyId))
                .andExpect(status().isNoContent());

        // Step 6: Verify vacancy is deleted
        mockMvc.perform(get("/api/v1/vacancies/" + vacancyId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateVacancyWithInvalidData() throws Exception {
        // Test with empty name
        Map<String, Object> invalidVacancy = new HashMap<>();
        invalidVacancy.put("name", "");
        invalidVacancy.put("criteria", Collections.emptyList());
        String invalidJson = objectMapper.writeValueAsString(invalidVacancy);

        mockMvc.perform(post("/api/v1/vacancies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());

        // Test with invalid criterion (negative weight)
        VacancyRequestDto invalidCriteriaDto = createVacancyWithInvalidCriterion();
        String invalidCriteriaJson = objectMapper.writeValueAsString(invalidCriteriaDto);

        mockMvc.perform(post("/api/v1/vacancies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidCriteriaJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateVacancyWithInvalidData() throws Exception {
        // First create a valid vacancy
        VacancyRequestDto validDto = createJuniorDeveloperVacancyDto();
        String validJson = objectMapper.writeValueAsString(validDto);

        String response = mockMvc.perform(post("/api/v1/vacancies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Vacancy createdVacancy = objectMapper.readValue(response, Vacancy.class);
        String vacancyId = createdVacancy.getId();

        // Now try to update with invalid data
        Map<String, Object> invalidUpdate = new HashMap<>();
        invalidUpdate.put("name", "");
        String invalidUpdateJson = objectMapper.writeValueAsString(invalidUpdate);

        mockMvc.perform(patch("/api/v1/vacancies/" + vacancyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidUpdateJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateNonExistentVacancy() throws Exception {
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("name", "Updated Name");
        String updateJson = objectMapper.writeValueAsString(updateRequest);

        mockMvc.perform(patch("/api/v1/vacancies/nonexistent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteNonExistentVacancy() throws Exception {
        mockMvc.perform(delete("/api/v1/vacancies/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateMultipleVacancies() throws Exception {
        // Create first vacancy
        VacancyRequestDto juniorDevDto = createJuniorDeveloperVacancyDto();
        String juniorDevJson = objectMapper.writeValueAsString(juniorDevDto);

        mockMvc.perform(post("/api/v1/vacancies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(juniorDevJson))
                .andExpect(status().isCreated());

        // Create second vacancy
        VacancyRequestDto seniorDevDto = createSeniorDeveloperVacancyDto();
        String seniorDevJson = objectMapper.writeValueAsString(seniorDevDto);

        mockMvc.perform(post("/api/v1/vacancies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(seniorDevJson))
                .andExpect(status().isCreated());

        // Verify we have two vacancies
        mockMvc.perform(get("/api/v1/vacancies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Junior Developer", "Senior Developer")));
    }

    private VacancyRequestDto createJuniorDeveloperVacancyDto() {
        Set<Criterion> criteria = new HashSet<>();

        Criterion ageCriterion = new Criterion(
                "AGE",
                3,
                Criterion.CriterionDetails.createObject("RANGE", new BigDecimal("22"), new BigDecimal("30"), null)
        );

        Criterion genderCriterion = new Criterion(
                "GENDER",
                1,
                Criterion.CriterionDetails.createObject("ANY", null, null, null)
        );

        Criterion salaryCriterion = new Criterion(
                "SALARY_RANGE",
                5,
                Criterion.CriterionDetails.createObject("RANGE", new BigDecimal("4500000"), new BigDecimal("6500000"), null)
        );

        criteria.add(ageCriterion);
        criteria.add(genderCriterion);
        criteria.add(salaryCriterion);

        return new VacancyRequestDto("Junior Developer", criteria);
    }

    private VacancyRequestDto createSeniorDeveloperVacancyDto() {
        Set<Criterion> criteria = new HashSet<>();

        Criterion ageCriterion = new Criterion(
                "AGE",
                2,
                Criterion.CriterionDetails.createObject("RANGE", new BigDecimal("30"), new BigDecimal("45"), null)
        );

        Criterion genderCriterion = new Criterion(
                "GENDER",
                3,
                Criterion.CriterionDetails.createObject("ENUMERATION", null, null, Set.of("MALE"))
        );

        Criterion salaryCriterion = new Criterion(
                "SALARY_RANGE",
                4,
                Criterion.CriterionDetails.createObject("RANGE", new BigDecimal("7000000"), new BigDecimal("12000000"), null)
        );

        criteria.add(ageCriterion);
        criteria.add(genderCriterion);
        criteria.add(salaryCriterion);

        return new VacancyRequestDto("Senior Developer", criteria);
    }

    private VacancyRequestDto createVacancyWithInvalidCriterion() {
        Set<Criterion> criteria = new HashSet<>();

        // Invalid criterion with negative weight
        Criterion invalidCriterion = new Criterion(
                "AGE",
                -1,
                Criterion.CriterionDetails.createObject("RANGE", new BigDecimal("20"), new BigDecimal("30"), null)
        );

        criteria.add(invalidCriterion);

        return new VacancyRequestDto("Invalid Vacancy", criteria);
    }
}

