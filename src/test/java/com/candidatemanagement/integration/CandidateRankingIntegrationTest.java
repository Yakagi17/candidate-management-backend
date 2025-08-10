package com.candidatemanagement.integration;

import com.candidatemanagement.dto.CandidateRequestDto;
import com.candidatemanagement.dto.VacancyRequestDto;
import com.candidatemanagement.model.Candidate;
import com.candidatemanagement.model.Vacancy;
import com.candidatemanagement.model.criteria.Criterion;
import com.candidatemanagement.repository.CandidateRepository;
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
import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@AutoConfigureWebMvc
class CandidateRankingIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.4.2");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private VacancyRepository vacancyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        candidateRepository.deleteAll();
        vacancyRepository.deleteAll();
    }

    @Test
    void testCandidateRankingForDifferentVacancies() throws Exception {
        // Create test candidates
        Map<String, String> candidateIds = createTestCandidates();

        // Create different vacancies with different criteria and test ranking
        String juniorVacancyId = createJuniorSoftwareEngineerVacancy();
        String seniorVacancyId = createSeniorDeveloperVacancy();
        String femaleOnlyVacancyId = createFemaleOnlyVacancy();

        // Test ranking for Junior Software Engineer
        mockMvc.perform(get("/api/v1/vacancies/" + juniorVacancyId + "/rank-candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                // Check that Indah and Siti have high scores
                .andExpect(jsonPath("$[0].score", is(9)))
                .andExpect(jsonPath("$[1].score", is(9)))
                // Check that Budi has low score
                .andExpect(jsonPath("$[2].score", is(1)));

        // Test ranking for Senior Developer
        mockMvc.perform(get("/api/v1/vacancies/" + seniorVacancyId + "/rank-candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                // Check that Budi is ranked highest
                .andExpect(jsonPath("$[0].name", is("Budi Santoso")))
                .andExpect(jsonPath("$[0].score", is(7)));

        // Test ranking for Female-only position
        mockMvc.perform(get("/api/v1/vacancies/" + femaleOnlyVacancyId + "/rank-candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                // Check only females have scores > 0
                .andExpect(jsonPath("$[0].score", greaterThan(0)))
                .andExpect(jsonPath("$[1].score", greaterThan(0)))
                .andExpect(jsonPath("$[2].score", is(0)))
                .andExpect(jsonPath("$[2].name", is("Budi Santoso")));
    }

    @Test
    void testRankingWithNoCandidates() throws Exception {
        // Create vacancy but no candidates
        String vacancyId = createJuniorSoftwareEngineerVacancy();

        // Test ranking with no candidates
        mockMvc.perform(get("/api/v1/vacancies/" + vacancyId + "/rank-candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testRankingWithNonExistentVacancy() throws Exception {
        // Create candidates but use non-existent vacancy ID
        createTestCandidates();

        // Test ranking with non-existent vacancy
        mockMvc.perform(get("/api/v1/vacancies/non-existent-id/rank-candidates"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testRankingWithInvalidCriteria() throws Exception {
        // Create candidates
        createTestCandidates();

        // Create vacancy with invalid criteria
        String invalidVacancyId = createVacancyWithInvalidCriteria();

        // Test ranking with invalid criteria
        mockMvc.perform(get("/api/v1/vacancies/" + invalidVacancyId + "/rank-candidates"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRankingAfterCandidateUpdate() throws Exception {
        // Create candidates and vacancy
        Map<String, String> candidateIds = createTestCandidates();
        String vacancyId = createJuniorSoftwareEngineerVacancy();

        // Update Budi's salary to match the criterion
        Map<String, Object> salaryUpdate = new HashMap<>();
        salaryUpdate.put("currentSalary", "5000000");
        String updateJson = objectMapper.writeValueAsString(salaryUpdate);

        mockMvc.perform(patch("/api/v1/candidates/" + candidateIds.get("Budi Santoso"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk());

        // Test ranking after update - Budi should now have a higher score
        mockMvc.perform(get("/api/v1/vacancies/" + vacancyId + "/rank-candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[2].name", not("Budi Santoso"))); // Budi should no longer be last
    }

    @Test
    void testRankingAfterVacancyUpdate() throws Exception {
        // Create candidates and vacancy
        createTestCandidates();
        String vacancyId = createJuniorSoftwareEngineerVacancy();

        // Get initial ranking
        mockMvc.perform(get("/api/v1/vacancies/" + vacancyId + "/rank-candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[2].name", is("Budi Santoso")));

        // Update vacancy criteria to favor higher salaries
        String vacancyResponse = mockMvc.perform(get("/api/v1/vacancies/" + vacancyId))
                .andReturn().getResponse().getContentAsString();

        Vacancy vacancy = objectMapper.readValue(vacancyResponse, Vacancy.class);

        // Change salary criterion to favor higher salaries
        for (Criterion criterion : vacancy.getCriteria()) {
            if ("SALARY_RANGE".equals(criterion.getName())) {
                criterion.setDetails(Criterion.CriterionDetails.createObject(
                        "RANGE", new BigDecimal("6000000"), new BigDecimal("9000000"), null));
            }
        }

        String updatedVacancyJson = objectMapper.writeValueAsString(vacancy);

        mockMvc.perform(patch("/api/v1/vacancies/" + vacancyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedVacancyJson))
                .andExpect(status().isOk());

        // Test ranking after update - Budi should now be ranked higher due to higher salary
        mockMvc.perform(get("/api/v1/vacancies/" + vacancyId + "/rank-candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name", is("Budi Santoso")));
    }

    private Map<String, String> createTestCandidates() throws Exception {
        Map<String, String> candidateIds = new HashMap<>();

        // Create Siti
        CandidateRequestDto siti = createTestCandidateDto("Siti Rahayu", "siti.r@example.com",
                LocalDate.of(1996, 5, 15), "FEMALE", new BigDecimal("5500000"));
        String sitiJson = objectMapper.writeValueAsString(siti);
        String sitiResponse = mockMvc.perform(post("/api/v1/candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sitiJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Candidate sitiCandidate = objectMapper.readValue(sitiResponse, Candidate.class);
        candidateIds.put("Siti Rahayu", sitiCandidate.getId());

        // Create Budi
        CandidateRequestDto budi = createTestCandidateDto("Budi Santoso", "budi.s@example.com",
                LocalDate.of(1989, 11, 20), "MALE", new BigDecimal("8000000"));
        String budiJson = objectMapper.writeValueAsString(budi);
        String budiResponse = mockMvc.perform(post("/api/v1/candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(budiJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Candidate budiCandidate = objectMapper.readValue(budiResponse, Candidate.class);
        candidateIds.put("Budi Santoso", budiCandidate.getId());

        // Create Indah
        CandidateRequestDto indah = createTestCandidateDto("Indah Lestari", "indah.l@example.com",
                LocalDate.of(2002, 3, 1), "FEMALE", new BigDecimal("4000000"));
        String indahJson = objectMapper.writeValueAsString(indah);
        String indahResponse = mockMvc.perform(post("/api/v1/candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(indahJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Candidate indahCandidate = objectMapper.readValue(indahResponse, Candidate.class);
        candidateIds.put("Indah Lestari", indahCandidate.getId());

        return candidateIds;
    }

    private String createJuniorSoftwareEngineerVacancy() throws Exception {
        // Create Junior Software Engineer vacancy
        Criterion ageCriterion = new Criterion("AGE", 3, Criterion.CriterionDetails.createObject(
                "RANGE", new BigDecimal("22"), new BigDecimal("30"), null));
        Criterion genderCriterion = new Criterion("GENDER", 1, Criterion.CriterionDetails.createObject(
                "ANY", null, null, null));
        Criterion salaryCriterion = new Criterion("SALARY_RANGE", 5, Criterion.CriterionDetails.createObject(
                "RANGE", new BigDecimal("4500000"), new BigDecimal("6500000"), null));

        VacancyRequestDto vacancyDto = new VacancyRequestDto(
            "Junior Software Engineer",
            Set.of(ageCriterion, genderCriterion, salaryCriterion)
        );

        String vacancyJson = objectMapper.writeValueAsString(vacancyDto);
        String response = mockMvc.perform(post("/api/v1/vacancies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(vacancyJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Vacancy vacancy = objectMapper.readValue(response, Vacancy.class);
        return vacancy.getId();
    }

    private String createSeniorDeveloperVacancy() throws Exception {
        // Create Senior Developer vacancy
        Criterion ageCriterion = new Criterion("AGE", 2, Criterion.CriterionDetails.createObject(
                "RANGE", new BigDecimal("30"), new BigDecimal("45"), null));
        Criterion genderCriterion = new Criterion("GENDER", 3, Criterion.CriterionDetails.createObject(
                "ENUMERATION", null, null, Set.of("MALE")));
        Criterion salaryCriterion = new Criterion("SALARY_RANGE", 4, Criterion.CriterionDetails.createObject(
                "RANGE", new BigDecimal("7000000"), new BigDecimal("10000000"), null));

        VacancyRequestDto vacancyDto = new VacancyRequestDto(
            "Senior Developer",
            Set.of(ageCriterion, genderCriterion, salaryCriterion)
        );

        String vacancyJson = objectMapper.writeValueAsString(vacancyDto);
        String response = mockMvc.perform(post("/api/v1/vacancies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(vacancyJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Vacancy vacancy = objectMapper.readValue(response, Vacancy.class);
        return vacancy.getId();
    }

    private String createFemaleOnlyVacancy() throws Exception {
        // Create Female-only position
        Criterion ageCriterion = new Criterion("AGE", 2, Criterion.CriterionDetails.createObject(
                "RANGE", new BigDecimal("20"), new BigDecimal("40"), null));
        Criterion genderCriterion = new Criterion("GENDER", 5, Criterion.CriterionDetails.createObject(
                "ENUMERATION", null, null, Set.of("FEMALE")));

        VacancyRequestDto vacancyDto = new VacancyRequestDto(
            "Female Only Position",
            Set.of(ageCriterion, genderCriterion)
        );

        String vacancyJson = objectMapper.writeValueAsString(vacancyDto);
        String response = mockMvc.perform(post("/api/v1/vacancies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(vacancyJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Vacancy vacancy = objectMapper.readValue(response, Vacancy.class);
        return vacancy.getId();
    }

    private String createVacancyWithInvalidCriteria() throws Exception {
        // Create vacancy with invalid criterion
        Criterion invalidCriterion = new Criterion("INVALID_FIELD", 1, Criterion.CriterionDetails.createObject(
                "INVALID_TYPE", null, null, null));

        VacancyRequestDto vacancyDto = new VacancyRequestDto(
            "Invalid Vacancy",
            Set.of(invalidCriterion)
        );

        String vacancyJson = objectMapper.writeValueAsString(vacancyDto);
        String response = mockMvc.perform(post("/api/v1/vacancies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(vacancyJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Vacancy vacancy = objectMapper.readValue(response, Vacancy.class);
        return vacancy.getId();
    }

    private CandidateRequestDto createTestCandidateDto(String name, String email, LocalDate birthdate,
                                                      String gender, BigDecimal salary) {
        return new CandidateRequestDto(name, email, birthdate, gender, salary);
    }
}

