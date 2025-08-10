package com.candidatemanagement.service;

import com.candidatemanagement.dto.VacancyRequestDto;
import com.candidatemanagement.model.Vacancy;

import java.util.List;
import java.util.Optional;

public interface VacancyService {

    Vacancy createVacancy(VacancyRequestDto vacancyDto);

    List<Vacancy> getAllVacancies();

    Optional<Vacancy> getVacancyById(String id);

    Vacancy updateVacancy(String id, VacancyRequestDto vacancyDto);

    void deleteVacancy(String id);
}

