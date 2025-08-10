package com.candidatemanagement.factory;

import com.candidatemanagement.model.criteria.Criterion;
import com.candidatemanagement.service.criteria.CriterionMatcher;
import com.candidatemanagement.service.criteria.impl.AnyCriterionMatcher;
import com.candidatemanagement.service.criteria.impl.EnumerationCriterionMatcher;
import com.candidatemanagement.service.criteria.impl.RangeCriterionMatcher;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CriterionMatcherFactory {

private final Map<String, CriterionMatcher> matcherMap;

    public CriterionMatcherFactory(AnyCriterionMatcher anyCriterionMatcher,
                               EnumerationCriterionMatcher enumerationCriterionMatcher,
                               RangeCriterionMatcher rangeCriterionMatcher) {
    this.matcherMap = Map.of(
            Criterion.CriterionType.ANY.name(), anyCriterionMatcher,
            Criterion.CriterionType.ENUMERATION.name(), enumerationCriterionMatcher,
            Criterion.CriterionType.RANGE.name(), rangeCriterionMatcher);
}

    public CriterionMatcher getMatcherByType(String type) {
        if (type == null) {
            return null;
        }
        return matcherMap.get(type.toUpperCase());
    }
}

