package com.candidatemanagement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CandidateRequestDto(
    String name,
    String email,
    LocalDate birthdate,
    String gender,
    BigDecimal currentSalary
) {}

