package com.candidatemanagement.service.criteria.impl;

import com.candidatemanagement.model.Candidate;
import com.candidatemanagement.model.criteria.Criterion;
import com.candidatemanagement.service.criteria.CriterionMatcher;
import org.springframework.stereotype.Component;

@Component
public class AnyCriterionMatcher implements CriterionMatcher {

    @Override
    public boolean matches(Candidate candidate, Criterion criterion) {
        return true;
    }

    @Override
    public String getSupportedType() {
        return Criterion.CriterionType.ANY.name();
    }
}

