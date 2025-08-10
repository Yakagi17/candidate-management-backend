package com.candidatemanagement.service.criteria;

import com.candidatemanagement.model.Candidate;
import com.candidatemanagement.model.criteria.Criterion;

public interface CriterionMatcher {
    
    boolean matches(Candidate candidate, Criterion criterion);
    
    String getSupportedType();
}
