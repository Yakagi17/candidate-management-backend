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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@AutoConfigureWebMvc
class CandidateManagementIntegrationTest {

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
    void testCompleteWorkflow() throws Exception {
        // Step 1: Create candidates
        CandidateRequestDto siti = createTestCandidateDto("Siti Rahayu", "siti.r@example.com",
                LocalDate.of(1996, 5, 15), "FEMALE", new BigDecimal("5500000"));
        
        CandidateRequestDto budi = createTestCandidateDto("Budi Santoso", "budi.s@example.com",
                LocalDate.of(1989, 11, 20), "MALE", new BigDecimal("8000000"));
        
        CandidateRequestDto indah = createTestCandidateDto("Indah Lestari", "indah.l@example.com",
                LocalDate.of(2002, 3, 1), "FEMALE", new BigDecimal("4000000"));

        String sitiJson = objectMapper.writeValueAsString(siti);
        String budiJson = objectMapper.writeValueAsString(budi);
        String indahJson = objectMapper.writeValueAsString(indah);

        // Create Siti
        mockMvc.perform(post("/api/v1/candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sitiJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Siti Rahayu")))
                .andExpect(jsonPath("$.email", is("siti.r@example.com")));

        // Create Budi
        mockMvc.perform(post("/api/v1/candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(budiJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Budi Santoso")));

        // Create Indah
        mockMvc.perform(post("/api/v1/candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(indahJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Indah Lestari")));

        // Step 2: Verify all candidates are created
        mockMvc.perform(get("/api/v1/candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        // Step 3: Create Junior Software Engineer vacancy
        VacancyRequestDto vacancyDto = createJuniorSoftwareEngineerVacancyDto();
        String vacancyJson = objectMapper.writeValueAsString(vacancyDto);

        String vacancyResponse = mockMvc.perform(post("/api/v1/vacancies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(vacancyJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Junior Software Engineer")))
                .andExpect(jsonPath("$.criteria", hasSize(3)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Vacancy createdVacancy = objectMapper.readValue(vacancyResponse, Vacancy.class);
        String vacancyId = createdVacancy.getId();

        // Step 4: Test candidate ranking
        mockMvc.perform(get("/api/v1/vacancies/" + vacancyId + "/rank-candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                // Check that Indah and Siti have score 9 (they should be at the top)
                .andExpect(jsonPath("$[0].score", is(9)))
                .andExpect(jsonPath("$[1].score", is(9)))
                // Check that Budi has score 1 (should be last)
                .andExpect(jsonPath("$[2].score", is(1)))
                .andExpect(jsonPath("$[2].name", is("Budi Santoso")));

        // Step 5: Test vacancy update
        Map<String, Object> vacancyUpdate = new HashMap<>();
        vacancyUpdate.put("name", "Junior Software Engineer - Updated");
        String updatedVacancyJson = objectMapper.writeValueAsString(vacancyUpdate);

        mockMvc.perform(patch("/api/v1/vacancies/" + vacancyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedVacancyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Junior Software Engineer - Updated")));

        // Step 6: Test candidate update
        Map<String, Object> candidateUpdate = new HashMap<>();
        candidateUpdate.put("currentSalary", "6000000");
        String updatedSitiJson = objectMapper.writeValueAsString(candidateUpdate);

        // Get Siti's ID first
        String candidatesResponse = mockMvc.perform(get("/api/v1/candidates"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Candidate[] candidates = objectMapper.readValue(candidatesResponse, Candidate[].class);
        String sitiId = Arrays.stream(candidates)
                .filter(c -> "Siti Rahayu".equals(c.getName()))
                .findFirst()
                .get()
                .getId();

        mockMvc.perform(patch("/api/v1/candidates/" + sitiId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedSitiJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentSalary", is(6000000)));

        // Step 7: Test get candidate by id
        mockMvc.perform(get("/api/v1/candidates/" + sitiId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Siti Rahayu")))
                .andExpect(jsonPath("$.currentSalary", is(6000000)));

        // Step 8: Test delete candidate
        mockMvc.perform(delete("/api/v1/candidates/" + sitiId))
                .andExpect(status().isNoContent());

        // Verify candidate is deleted
        mockMvc.perform(get("/api/v1/candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // Step 9: Test delete vacancy
        mockMvc.perform(delete("/api/v1/vacancies/" + vacancyId))
                .andExpect(status().isNoContent());

        // Verify vacancy is deleted
        mockMvc.perform(get("/api/v1/vacancies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testValidationErrors() throws Exception {
        // Test invalid candidate creation - missing required fields
        Map<String, Object> invalidCandidate = new HashMap<>();
        invalidCandidate.put("name", ""); // Invalid: empty name
        invalidCandidate.put("email", "invalid-email"); // Invalid: not a valid email
        invalidCandidate.put("gender", "INVALID"); // Invalid: not MALE or FEMALE

        String invalidJson = objectMapper.writeValueAsString(invalidCandidate);

        mockMvc.perform(post("/api/v1/candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.httpStatus", is("BAD_REQUEST")));

        // Test invalid vacancy creation
        Map<String, Object> invalidVacancy = new HashMap<>();
        invalidVacancy.put("name", ""); // Invalid: empty name

        String invalidVacancyJson = objectMapper.writeValueAsString(invalidVacancy);

        mockMvc.perform(post("/api/v1/vacancies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidVacancyJson))
                .andExpect(status().isBadRequest());

        // Test candidate with negative salary
        CandidateRequestDto negativeSalaryCandidate = createTestCandidateDto(
                "Test Name",
                "test@example.com",
                LocalDate.of(1990, 1, 1),
                "MALE",
                new BigDecimal("-5000")
        );

        String negativeSalaryJson = objectMapper.writeValueAsString(negativeSalaryCandidate);

        mockMvc.perform(post("/api/v1/candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(negativeSalaryJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDuplicateEmailError() throws Exception {
        // Create first candidate
        CandidateRequestDto candidate1 = createTestCandidateDto(
                "John Doe",
                "john@example.com",
                LocalDate.of(1990, 1, 1),
                "MALE",
                new BigDecimal("5000000")
        );

        String candidateJson = objectMapper.writeValueAsString(candidate1);

        mockMvc.perform(post("/api/v1/candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(candidateJson))
                .andExpect(status().isCreated());

        // Try to create second candidate with same email
        CandidateRequestDto candidate2 = createTestCandidateDto(
                "Jane Doe",
                "john@example.com",
                LocalDate.of(1992, 1, 1),
                "FEMALE",
                new BigDecimal("5500000")
        );

        String candidate2Json = objectMapper.writeValueAsString(candidate2);

        mockMvc.perform(post("/api/v1/candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(candidate2Json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("Duplicate Email")));
    }

    @Test
    void testNotFoundErrors() throws Exception {
        // Test get non-existent candidate
        mockMvc.perform(get("/api/v1/candidates/nonexistent"))
                .andExpect(status().isNotFound());

        // Test update non-existent candidate
        CandidateRequestDto updateDto = createTestCandidateDto(
                "Updated Name",
                "updated@example.com",
                null,
                null,
                null
        );

        String updateJson = objectMapper.writeValueAsString(updateDto);

        mockMvc.perform(patch("/api/v1/candidates/nonexistent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isNotFound());

        // Test delete non-existent candidate
        mockMvc.perform(delete("/api/v1/candidates/nonexistent"))
                .andExpect(status().isNotFound());

        // Test get non-existent vacancy
        mockMvc.perform(get("/api/v1/vacancies/nonexistent"))
                .andExpect(status().isNotFound());

        // Test rank candidates for non-existent vacancy
        mockMvc.perform(get("/api/v1/vacancies/nonexistent/rank-candidates"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testPartialUpdate() throws Exception {
        // Create a candidate
        CandidateRequestDto candidate = createTestCandidateDto(
                "Original Name",
                "original@example.com",
                LocalDate.of(1990, 1, 1),
                "MALE",
                new BigDecimal("5000000")
        );

        String createJson = objectMapper.writeValueAsString(candidate);

        String response = mockMvc.perform(post("/api/v1/candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Candidate createdCandidate = objectMapper.readValue(response, Candidate.class);
        String candidateId = createdCandidate.getId();

        // Update only the name
        Map<String, Object> nameUpdate = new HashMap<>();
        nameUpdate.put("name", "Updated Name");
        String nameUpdateJson = objectMapper.writeValueAsString(nameUpdate);

        mockMvc.perform(patch("/api/v1/candidates/" + candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(nameUpdateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Name")))
                .andExpect(jsonPath("$.email", is("original@example.com"))) // unchanged
                .andExpect(jsonPath("$.currentSalary", is(5000000))); // unchanged

        // Update only the salary
        Map<String, Object> salaryUpdate = new HashMap<>();
        salaryUpdate.put("currentSalary", "6000000");
        String salaryUpdateJson = objectMapper.writeValueAsString(salaryUpdate);

        mockMvc.perform(patch("/api/v1/candidates/" + candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(salaryUpdateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Name"))) // unchanged from previous update
                .andExpect(jsonPath("$.currentSalary", is(6000000))); // updated
    }

    @Test
    void testInvalidUpdateData() throws Exception {
        // Create a candidate
        CandidateRequestDto candidate = createTestCandidateDto(
                "Test Candidate",
                "test@example.com",
                LocalDate.of(1990, 1, 1),
                "MALE",
                new BigDecimal("5000000")
        );

        String createJson = objectMapper.writeValueAsString(candidate);

        String response = mockMvc.perform(post("/api/v1/candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Candidate createdCandidate = objectMapper.readValue(response, Candidate.class);
        String candidateId = createdCandidate.getId();

        // Update with invalid email
        Map<String, Object> invalidEmail = new HashMap<>();
        invalidEmail.put("email", "not-an-email");
        String invalidEmailJson = objectMapper.writeValueAsString(invalidEmail);

        mockMvc.perform(patch("/api/v1/candidates/" + candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidEmailJson))
                .andExpect(status().isBadRequest());

        // Update with negative salary
        Map<String, Object> invalidSalary = new HashMap<>();
        invalidSalary.put("currentSalary", "-1000");
        String invalidSalaryJson = objectMapper.writeValueAsString(invalidSalary);

        mockMvc.perform(patch("/api/v1/candidates/" + candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidSalaryJson))
                .andExpect(status().isBadRequest());

        // Update with invalid gender
        Map<String, Object> invalidGender = new HashMap<>();
        invalidGender.put("gender", "UNKNOWN");
        String invalidGenderJson = objectMapper.writeValueAsString(invalidGender);

        mockMvc.perform(patch("/api/v1/candidates/" + candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidGenderJson))
                .andExpect(status().isBadRequest());
    }

    private CandidateRequestDto createTestCandidateDto(String name, String email, LocalDate birthdate,
                                        String gender, BigDecimal salary) {
        return new CandidateRequestDto(name, email, birthdate, gender, salary);
    }

    private VacancyRequestDto createJuniorSoftwareEngineerVacancyDto() {
        // Create criteria list
        Criterion ageCriterion = new Criterion("AGE", 3, Criterion.CriterionDetails.createObject(
                "RANGE", new BigDecimal("22"), new BigDecimal("30"), null));
        Criterion genderCriterion = new Criterion("GENDER", 1, Criterion.CriterionDetails.createObject(
                "ENUMERATION", null, null, null));
        Criterion salaryCriterion = new Criterion("SALARY_RANGE", 5, Criterion.CriterionDetails.createObject(
                "RANGE", new BigDecimal("4500000"), new BigDecimal("6500000"), null));

        return new VacancyRequestDto(
            "Junior Software Engineer",
            Set.of(ageCriterion, genderCriterion, salaryCriterion)
        );
    }
}
