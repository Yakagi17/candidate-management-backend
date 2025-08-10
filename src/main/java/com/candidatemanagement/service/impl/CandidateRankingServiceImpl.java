package com.candidatemanagement.service.impl;

import com.candidatemanagement.dto.CandidateRankingDto;
import com.candidatemanagement.exception.GenericApiException;
import com.candidatemanagement.factory.CriterionMatcherFactory;
import com.candidatemanagement.model.Candidate;
import com.candidatemanagement.model.Vacancy;
import com.candidatemanagement.model.criteria.Criterion;
import com.candidatemanagement.service.CandidateRankingService;
import com.candidatemanagement.service.CandidateService;
import com.candidatemanagement.service.VacancyService;
import com.candidatemanagement.service.criteria.CriterionMatcher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CandidateRankingServiceImpl implements CandidateRankingService {

    private final CandidateService candidateService;
    private final VacancyService vacancyService;
    private final CriterionMatcherFactory criterionMatcherFactory;

    public CandidateRankingServiceImpl(
            CandidateService candidateService,
            VacancyService vacancyService,
            CriterionMatcherFactory criterionMatcherFactory) {
        this.candidateService = candidateService;
        this.vacancyService = vacancyService;
        this.criterionMatcherFactory = criterionMatcherFactory;
    }

    @Override
    public List<CandidateRankingDto> rankCandidatesForVacancy(String vacancyId) {
        Optional<Vacancy> vacancyOpt = vacancyService.getVacancyById(vacancyId);
        if (vacancyOpt.isEmpty()) {
            throw new GenericApiException(
                HttpStatus.NOT_FOUND,
                "Vacancy Not Found",
                "Vacancy with id " + vacancyId + " not found"
            );
        }

        Vacancy vacancy = vacancyOpt.get();
        List<Candidate> allCandidates = candidateService.getAllCandidates();

        return allCandidates.stream()
                .map(candidate -> {
                    int score = calculateScore(candidate, vacancy);
                    return new CandidateRankingDto(
                            candidate.getId(),
                            candidate.getName(),
                            candidate.getEmail(),
                            score
                    );
                })
                .sorted((c1, c2) -> Integer.compare(c2.score(), c1.score()))
                .collect(Collectors.toList());
    }

    private int calculateScore(Candidate candidate, Vacancy vacancy) {
        int totalScore = 0;

        for (Criterion criterion : vacancy.getCriteria()) {
            if (criterion.getDetails() == null || criterion.getDetails().getType() == null) {
                continue;
            }

            CriterionMatcher matcher = criterionMatcherFactory.getMatcherByType(criterion.getDetails().getType());
            if (matcher == null) {
                throw new GenericApiException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Criterion",
                    "No matcher found for criterion type: " + criterion.getDetails().getType()
                );
            }

            if (matcher.matches(candidate, criterion)) {
                totalScore += criterion.getWeight();
            }
        }

        return totalScore;
    }
}
