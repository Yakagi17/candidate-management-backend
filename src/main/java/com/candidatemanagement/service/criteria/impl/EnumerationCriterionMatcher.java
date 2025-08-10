package com.candidatemanagement.service.criteria.impl;

import com.candidatemanagement.enums.Gender;
import com.candidatemanagement.model.Candidate;
import com.candidatemanagement.model.criteria.Criteria;
import com.candidatemanagement.model.criteria.Criterion;
import com.candidatemanagement.service.criteria.CriterionMatcher;
import com.candidatemanagement.utils.ValidationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.candidatemanagement.utils.ValidationUtils.findField;

@Component
public class EnumerationCriterionMatcher implements CriterionMatcher {

    @Override
    public boolean matches(Candidate candidate, Criterion criterion) {
        if (criterion == null || criterion.getDetails() == null ||
            criterion.getDetails().getOptions() == null ||
            criterion.getDetails().getOptions().isEmpty()) {
            return false;
        }

        Set<String> options = criterion.getDetails().getOptions()
                .stream().map(String::toLowerCase).collect(Collectors.toSet());

        String fieldName = criterion.getName().toLowerCase();
        Field field = findField(candidate.getClass(), fieldName);
        if (field == null) {
            if (Criteria.CUSTOM_CRITERIA_FIELDS.contains(fieldName)) {
                String methodName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                try {
                    java.lang.reflect.Method method = candidate.getClass().getMethod(methodName);
                    Object value = method.invoke(candidate);
                    if (value == null) {
                        return false;
                    }
                    String candidateValue = value.toString().toLowerCase();
                    return options.contains(candidateValue);
                } catch (Exception e) {
                    // Log the exception or handle it
                    // TODO: log the exception
                    return false;
                }
            } else {
                return false;
            }

        }

        List<String> enumValues;
        if (ValidationUtils.isEnumFieldInCriteria(fieldName, field)) {
            enumValues = ValidationUtils.getEnumValuesFromClass(field.getType())
                    .stream()
                    .filter(value -> options.contains(value.toLowerCase()))
                    .toList();

        } else {
            enumValues = List.copyOf(options);
        }

        if (enumValues.isEmpty()) {
            return false;
        }

        field.setAccessible(true);
        try {
            Object value = field.get(candidate);
            if (value == null) {
                return false;
            }
            String candidateValue = value.toString().toLowerCase();

            return enumValues.contains(candidateValue);
        } catch (IllegalAccessException e) {
            // Log the exception or handle it
            //TODO: log the exception
            return false;
        }
    }

    @Override
    public String getSupportedType() {
        return Criterion.CriterionType.ENUMERATION.name();
    }
}

