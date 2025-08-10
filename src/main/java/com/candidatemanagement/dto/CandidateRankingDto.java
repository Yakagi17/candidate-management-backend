package com.candidatemanagement.dto;

public record CandidateRankingDto(
    int rank,
    String id,
    String name,
    String email,
    int score
) {}
