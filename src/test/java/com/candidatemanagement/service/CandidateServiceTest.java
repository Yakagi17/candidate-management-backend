package com.candidatemanagement.service;

import com.candidatemanagement.dto.CandidateRequestDto;
import com.candidatemanagement.enums.Gender;
import com.candidatemanagement.exception.GenericApiException;
import com.candidatemanagement.exception.ValidationException;
import com.candidatemanagement.model.Candidate;
import com.candidatemanagement.repository.CandidateRepository;
import com.candidatemanagement.service.impl.CandidateServiceImpl;
import com.candidatemanagement.utils.ValidationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateServiceTest {

    @Mock
    private CandidateRepository candidateRepository;

    @InjectMocks
    private CandidateServiceImpl candidateService;

    private Candidate testCandidate;
    private CandidateRequestDto testCandidateDto;
    private CandidateRequestDto invalidEmailDto;
    private CandidateRequestDto invalidSalaryDto;
    private CandidateRequestDto invalidGenderDto;

    @BeforeEach
    void setUp() {
        testCandidate = new Candidate(
            "John Doe",
            "john.doe@example.com",
            LocalDate.of(1990, 1, 1),
            Gender.MALE,
            new BigDecimal("5000000")
        );
        testCandidate.setId("1");

        testCandidateDto = new CandidateRequestDto(
                "John Doe",
                "john.doe@example.com",
                LocalDate.of(1990, 1, 1),
                "MALE",
                new BigDecimal("5000000")
        );

        invalidEmailDto = new CandidateRequestDto(
                "John Doe",
                "invalid-email",
                LocalDate.of(1990, 1, 1),
                "MALE",
                new BigDecimal("5000000")
        );

        invalidSalaryDto = new CandidateRequestDto(
                "John Doe",
                "john.doe@example.com",
                LocalDate.of(1990, 1, 1),
                "MALE",
                new BigDecimal("-5000")
        );

        invalidGenderDto = new CandidateRequestDto(
                "John Doe",
                "john.doe@example.com",
                LocalDate.of(1990, 1, 1),
                "UNKNOWN",
                new BigDecimal("5000000")
        );
    }

    @Test
    void createCandidate_Success() {
        when(candidateRepository.existsByEmail(anyString())).thenReturn(false);
        when(candidateRepository.save(any(Candidate.class))).thenReturn(testCandidate);

        Candidate result = candidateService.createCandidate(testCandidateDto);

        assertNotNull(result);
        assertEquals(testCandidateDto.name(), result.getName());
        assertEquals(testCandidateDto.email(), result.getEmail());
        verify(candidateRepository).save(any(Candidate.class));
    }

    @Test
    void createCandidate_EmailAlreadyExists_ThrowsException() {
        when(candidateRepository.existsByEmail(testCandidateDto.email())).thenReturn(true);

        GenericApiException exception = assertThrows(GenericApiException.class, () -> {
            candidateService.createCandidate(testCandidateDto);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Duplicate Email", exception.getTitle());
        assertTrue(exception.getMessage().contains(testCandidateDto.email()));
        verify(candidateRepository, never()).save(any(Candidate.class));
    }

    @Test
    void createCandidate_InvalidEmail_ThrowsException() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            candidateService.createCandidate(invalidEmailDto);
        });

        assertNotNull(exception.getErrors());
        assertFalse(exception.getErrors().isEmpty());
        assertTrue(exception.getErrors().stream()
                .anyMatch(error -> error.field().equalsIgnoreCase("email")));
    }

    @Test
    void createCandidate_NegativeSalary_ThrowsException() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            candidateService.createCandidate(invalidSalaryDto);
        });

        assertNotNull(exception.getErrors());
        assertFalse(exception.getErrors().isEmpty());
        assertTrue(exception.getErrors().stream()
                .anyMatch(error -> error.field().equalsIgnoreCase("currentSalary") &&
                        error.message().contains("must be positive")));
    }

    @Test
    void createCandidate_InvalidGender_ThrowsException() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            candidateService.createCandidate(invalidGenderDto);
        });

        assertNotNull(exception.getErrors());
        assertFalse(exception.getErrors().isEmpty());
        assertTrue(exception.getErrors().stream()
                .anyMatch(error -> error.field().equalsIgnoreCase("gender")));
    }

    @Test
    void getAllCandidates_Success() {
        List<Candidate> candidates = Collections.singletonList(testCandidate);
        when(candidateRepository.findAll()).thenReturn(candidates);

        List<Candidate> result = candidateService.getAllCandidates();

        assertEquals(1, result.size());
        assertEquals(testCandidate.getName(), result.get(0).getName());
    }

    @Test
    void getAllCandidates_EmptyList() {
        when(candidateRepository.findAll()).thenReturn(List.of());

        List<Candidate> result = candidateService.getAllCandidates();

        assertTrue(result.isEmpty());
    }

    @Test
    void getCandidateById_Success() {
        when(candidateRepository.findById("1")).thenReturn(Optional.of(testCandidate));

        Optional<Candidate> result = candidateService.getCandidateById("1");

        assertTrue(result.isPresent());
        assertEquals(testCandidate.getName(), result.get().getName());
    }

    @Test
    void getCandidateById_NotFound() {
        when(candidateRepository.findById("nonexistent")).thenReturn(Optional.empty());

        Optional<Candidate> result = candidateService.getCandidateById("nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    void updateCandidate_Success() {
        CandidateRequestDto updatedDto = new CandidateRequestDto(
                "John Updated",
                "john.updated@example.com",
                null,
                null,
                new BigDecimal("6000000")
        );

        Candidate updatedCandidate = new Candidate(
            "John Updated",
            "john.updated@example.com",
            LocalDate.of(1990, 1, 1),
            Gender.MALE,
            new BigDecimal("6000000")
        );
        updatedCandidate.setId("1");

        when(candidateRepository.findById("1")).thenReturn(Optional.of(testCandidate));
        when(candidateRepository.existsByEmail(updatedDto.email())).thenReturn(false);
        when(candidateRepository.save(any(Candidate.class))).thenReturn(updatedCandidate);

        Candidate result = candidateService.updateCandidate("1", updatedDto);

        assertNotNull(result);
        assertEquals("John Updated", result.getName());
        assertEquals("john.updated@example.com", result.getEmail());
        assertEquals(new BigDecimal("6000000"), result.getCurrentSalary());
        verify(candidateRepository).save(any(Candidate.class));
    }

    @Test
    void updateCandidate_PartialUpdate_OnlyName() {
        CandidateRequestDto partialDto = new CandidateRequestDto(
                "John Updated",
                null,
                null,
                null,
                null
        );

        Candidate expectedResult = new Candidate(
            "John Updated",
            "john.doe@example.com",
            LocalDate.of(1990, 1, 1),
            Gender.MALE,
            new BigDecimal("5000000")
        );
        expectedResult.setId("1");

        when(candidateRepository.findById("1")).thenReturn(Optional.of(testCandidate));
        when(candidateRepository.save(any(Candidate.class))).thenReturn(expectedResult);

        Candidate result = candidateService.updateCandidate("1", partialDto);

        assertNotNull(result);
        assertEquals("John Updated", result.getName());
        assertEquals("john.doe@example.com", result.getEmail()); // unchanged
        assertEquals(new BigDecimal("5000000"), result.getCurrentSalary()); // unchanged
        verify(candidateRepository).save(any(Candidate.class));
    }

    @Test
    void updateCandidate_NotFound_ThrowsException() {
        when(candidateRepository.findById("1")).thenReturn(Optional.empty());

        GenericApiException exception = assertThrows(GenericApiException.class, () -> {
            candidateService.updateCandidate("1", testCandidateDto);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Resource Not Found", exception.getTitle());
    }

    @Test
    void updateCandidate_EmailAlreadyExists_ThrowsException() {
        CandidateRequestDto updatedDto = new CandidateRequestDto(
                "John Doe",
                "existing@example.com",
                null,
                null,
                null
        );

        when(candidateRepository.findById("1")).thenReturn(Optional.of(testCandidate));
        when(candidateRepository.existsByEmail("existing@example.com")).thenReturn(true);

        GenericApiException exception = assertThrows(GenericApiException.class, () -> {
            candidateService.updateCandidate("1", updatedDto);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Duplicate Email", exception.getTitle());
    }

    @Test
    void deleteCandidate_Success() {
        when(candidateRepository.existsById("1")).thenReturn(true);
        doNothing().when(candidateRepository).deleteById("1");

        candidateService.deleteCandidate("1");

        verify(candidateRepository).deleteById("1");
    }

    @Test
    void deleteCandidate_NotFound_ThrowsException() {
        when(candidateRepository.existsById("1")).thenReturn(false);

        GenericApiException exception = assertThrows(GenericApiException.class, () -> {
            candidateService.deleteCandidate("1");
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Resource Not Found", exception.getTitle());
    }
}
