package com.candidatemanagement.service;

import com.candidatemanagement.dto.VacancyRequestDto;
import com.candidatemanagement.exception.GenericApiException;
import com.candidatemanagement.exception.ValidationException;
import com.candidatemanagement.model.Vacancy;
import com.candidatemanagement.model.criteria.Criterion;
import com.candidatemanagement.repository.VacancyRepository;
import com.candidatemanagement.service.impl.VacancyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VacancyServiceTest {

    @Mock
    private VacancyRepository vacancyRepository;

    @InjectMocks
    private VacancyServiceImpl vacancyService;

    private Set<Criterion> validCriteria;
    private VacancyRequestDto validVacancyDto;
    private VacancyRequestDto invalidVacancyDto;
    private Vacancy testVacancy;

    @BeforeEach
    void setUp() {
        // Create valid test data
        validCriteria = new HashSet<>();
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

        Criterion genderCriterion = new Criterion(
                "gender",
                2,
                Criterion.CriterionDetails.createObject("ANY", null, null, null)
        );

        validCriteria.add(salaryRangeCriterion);
        validCriteria.add(ageCriterion);
        validCriteria.add(genderCriterion);

        validVacancyDto = new VacancyRequestDto("Software Engineer", validCriteria);

        Set<Criterion> invalidCriteria = new HashSet<>();
        Criterion invalidCriterion = new Criterion(
                "",  // Empty name is invalid
                0,   // Invalid weight
                Criterion.CriterionDetails.createObject("RANGE", null, null, null)  // Valid type but missing required values
        );
        invalidCriteria.add(invalidCriterion);

        invalidVacancyDto = new VacancyRequestDto("", invalidCriteria);

        // Create test vacancy
        testVacancy = new Vacancy("Software Engineer", validCriteria);
        testVacancy.setId("1");
    }

    @Test
    void createVacancy_Success() {
        when(vacancyRepository.save(any(Vacancy.class))).thenReturn(testVacancy);

        Vacancy result = vacancyService.createVacancy(validVacancyDto);

        assertNotNull(result);
        assertEquals("Software Engineer", result.getName());
        assertEquals(3, result.getCriteria().size());
        verify(vacancyRepository).save(any(Vacancy.class));
    }

    @Test
    void createVacancy_InvalidName_ThrowsException() {
        VacancyRequestDto invalidNameDto = new VacancyRequestDto("", validVacancyDto.criteria());

        assertThrows(ValidationException.class, () -> {
            vacancyService.createVacancy(invalidNameDto);
        });

        verify(vacancyRepository, never()).save(any(Vacancy.class));
    }

    @Test
    void createVacancy_InvalidCriteria_ThrowsException() {
        assertThrows(ValidationException.class, () -> {
            vacancyService.createVacancy(invalidVacancyDto);
        });

        verify(vacancyRepository, never()).save(any(Vacancy.class));
    }

    @Test
    void getAllVacancies_Success() {
        List<Vacancy> vacancies = Arrays.asList(testVacancy);
        when(vacancyRepository.findAll()).thenReturn(vacancies);

        List<Vacancy> result = vacancyService.getAllVacancies();

        assertEquals(1, result.size());
        assertEquals("Software Engineer", result.get(0).getName());
    }

    @Test
    void getAllVacancies_EmptyList() {
        when(vacancyRepository.findAll()).thenReturn(Collections.emptyList());

        List<Vacancy> result = vacancyService.getAllVacancies();

        assertTrue(result.isEmpty());
    }

    @Test
    void getVacancyById_Success() {
        when(vacancyRepository.findById("1")).thenReturn(Optional.of(testVacancy));

        Optional<Vacancy> result = vacancyService.getVacancyById("1");

        assertTrue(result.isPresent());
        assertEquals("Software Engineer", result.get().getName());
    }

    @Test
    void getVacancyById_NotFound() {
        when(vacancyRepository.findById("nonexistent")).thenReturn(Optional.empty());

        Optional<Vacancy> result = vacancyService.getVacancyById("nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    void updateVacancy_Success() {
        // Create updated DTO with new name
        VacancyRequestDto updatedDto = new VacancyRequestDto(
            "Senior Software Engineer",
            validVacancyDto.criteria()
        );

        Vacancy updatedVacancy = new Vacancy(
            "Senior Software Engineer",
            validVacancyDto.criteria()
        );
        updatedVacancy.setId("1");

        when(vacancyRepository.findById("1")).thenReturn(Optional.of(testVacancy));
        when(vacancyRepository.save(any(Vacancy.class))).thenReturn(updatedVacancy);

        Vacancy result = vacancyService.updateVacancy("1", updatedDto);

        assertNotNull(result);
        assertEquals("Senior Software Engineer", result.getName());
        verify(vacancyRepository).save(any(Vacancy.class));
    }

    @Test
    void updateVacancy_PartialUpdate() {
        VacancyRequestDto partialUpdateDto = new VacancyRequestDto(
                "Updated Name",
                null
        );

        Vacancy expectedResult = new Vacancy(
                "Updated Name",
                validCriteria  // Use the class variable for consistency
        );
        expectedResult.setId("1");

        when(vacancyRepository.findById("1")).thenReturn(Optional.of(testVacancy));
        when(vacancyRepository.save(any(Vacancy.class))).thenReturn(expectedResult);

        Vacancy result = vacancyService.updateVacancy("1", partialUpdateDto);

        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
        assertEquals(validCriteria, result.getCriteria());  // Use validCriteria for verification
        verify(vacancyRepository).save(any(Vacancy.class));
    }

    @Test
    void updateVacancy_InvalidData_ThrowsException() {
        when(vacancyRepository.findById("1")).thenReturn(Optional.of(testVacancy));

        assertThrows(ValidationException.class, () -> {
            vacancyService.updateVacancy("1", invalidVacancyDto);
        });

        verify(vacancyRepository, never()).save(any(Vacancy.class));
    }

    @Test
    void updateVacancy_NotFound_ThrowsException() {
        when(vacancyRepository.findById("nonexistent")).thenReturn(Optional.empty());

        GenericApiException exception = assertThrows(GenericApiException.class, () -> {
            vacancyService.updateVacancy("nonexistent", validVacancyDto);
        });

        assertEquals("Vacancy Not Found", exception.getTitle());
        verify(vacancyRepository, never()).save(any(Vacancy.class));
    }

    @Test
    void deleteVacancy_Success() {
        when(vacancyRepository.existsById("1")).thenReturn(true);
        doNothing().when(vacancyRepository).deleteById("1");

        vacancyService.deleteVacancy("1");

        verify(vacancyRepository).deleteById("1");
    }

    @Test
    void deleteVacancy_NotFound_ThrowsException() {
        when(vacancyRepository.existsById("nonexistent")).thenReturn(false);

        GenericApiException exception = assertThrows(GenericApiException.class, () -> {
            vacancyService.deleteVacancy("nonexistent");
        });

        assertEquals("Vacancy Not Found", exception.getTitle());
        verify(vacancyRepository, never()).deleteById(anyString());
    }
}

