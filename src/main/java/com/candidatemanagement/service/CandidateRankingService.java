package com.candidatemanagement.service;

import com.candidatemanagement.dto.CandidateRankingDto;
import java.util.List;

public interface CandidateRankingService {

    List<CandidateRankingDto> rankCandidatesForVacancy(String vacancyId);
}
