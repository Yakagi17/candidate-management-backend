package com.candidatemanagement.service.criteria.impl;

import com.candidatemanagement.model.Candidate;
import com.candidatemanagement.model.criteria.Criteria;
import com.candidatemanagement.model.criteria.Criterion;
import com.candidatemanagement.service.criteria.CriterionMatcher;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;

import static com.candidatemanagement.utils.ValidationUtils.findField;

@Component
public class RangeCriterionMatcher implements CriterionMatcher {

    @Override
    public boolean matches(Candidate candidate, Criterion criterion) {
        if (criterion == null || criterion.getDetails() == null) {
            return false;
        }

        String fieldName = criterion.getName();
        BigDecimal minValue = criterion.getDetails().getMinValue();
        BigDecimal maxValue = criterion.getDetails().getMaxValue();

        Field field = findField(candidate.getClass(), fieldName);
        boolean isCustomField = Criteria.CUSTOM_CRITERIA_FIELDS.contains(fieldName);
        if (isCustomField) {
            String methodName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            try {
                java.lang.reflect.Method method = candidate.getClass().getMethod(methodName);
                Object value = method.invoke(candidate);
                if (value == null) {
                    return false;
                }

                if (value instanceof BigDecimal candidateValue) {
                    return isInRange(candidateValue, minValue, maxValue);
                } else if (value instanceof Number) {
                    // Convert Number to BigDecimal for comparison
                    BigDecimal candidateValue = BigDecimal.valueOf(((Number) value).doubleValue());
                    return isInRange(candidateValue, minValue, maxValue);
                } else {
                    // Handle other types as needed
                    return false;
                }
            } catch (Exception e) {
                // Log the exception or handle it
                // TODO: log the exception
                return false;
            }
        }

        if (field == null) {
            return false;
        }

        try {
            field.setAccessible(true);
            Object value = field.get(candidate);
            if (value == null) {
                return false;
            }

            if (value instanceof BigDecimal candidateValue) {
                return isInRange(candidateValue, minValue, maxValue);
            } else if (value instanceof Number) {
                BigDecimal candidateValue = BigDecimal.valueOf(((Number) value).doubleValue());
                return isInRange(candidateValue, minValue, maxValue);
            } else if (value instanceof LocalDate) {
                return false;
            } else {
                return false;
            }
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    private boolean isInRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (min == null && max == null) {
            return true;
        }

        if (min == null) {
            return value.compareTo(max) <= 0;
        }

        if (max == null) {
            return value.compareTo(min) >= 0;
        }

        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }

    @Override
    public String getSupportedType() {
        return Criterion.CriterionType.RANGE.name();
    }
}

