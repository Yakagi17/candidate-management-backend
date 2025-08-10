package com.candidatemanagement.dto;

public record CandidateScore(
        String id,
        String name,
        String email,
        int score
) {}