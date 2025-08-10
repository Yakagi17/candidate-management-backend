package com.candidatemanagement.service;

import com.candidatemanagement.dto.CandidateRequestDto;
import com.candidatemanagement.model.Candidate;

import java.util.List;
import java.util.Optional;

public interface CandidateService {
    Candidate createCandidate(CandidateRequestDto candidateDto);
    List<Candidate> getAllCandidates();
    Optional<Candidate> getCandidateById(String id);
    Candidate updateCandidate(String id, CandidateRequestDto candidateDto);
    void deleteCandidate(String id);
}
