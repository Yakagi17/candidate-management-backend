package com.candidatemanagement.dto;

import com.candidatemanagement.model.criteria.Criterion;

import java.math.BigDecimal;
import java.util.Set;

public record VacancyRequestDto(
    String name,
    Set<Criterion> criteria
) {}

