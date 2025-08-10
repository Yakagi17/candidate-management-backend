package com.candidatemanagement.repository;

import com.candidatemanagement.model.Candidate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateRepository extends MongoRepository<Candidate, String> {
    
    Optional<Candidate> findByEmail(String email);
    
    boolean existsByEmail(String email);
}
