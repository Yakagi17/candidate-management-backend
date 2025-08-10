package com.candidatemanagement.utils;

import com.candidatemanagement.exception.ValidationException;
import com.candidatemanagement.model.criteria.Criterion;
import com.candidatemanagement.model.criteria.Criteria;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

public class ValidationUtils {

    public enum ValidationType {
        NOT_NULL,
        NOT_EMPTY,
        NOT_BLANK,
        VALID_EMAIL,
        POSITIVE,
        ENUM_VALUE
    }

    private static final List<String> CRITERIA_NAMES = Arrays.stream(Criteria.class.getDeclaredFields())
        .map(Field::getName)
        .toList();

    private static final List<String> CRITERION_DETAILS_TYPES = Arrays.stream(Criterion.CriterionType.values())
        .map(Enum::name)
        .toList();

    private static final String CRITERIA_MAP_CLASS_NAME = "className";
    private static final String CRITERIA_MAP_IS_NUMERIC = "isNumeric";
    private static final String CRITERIA_MAP_IS_VALID_ENUM = "isValidEnum";

    public record FieldValidation(String fieldName, List<ValidationType> validations, Class<? extends Enum<?>> enumClass) {
        public FieldValidation(String fieldName, List<ValidationType> validations) {
            this(fieldName, validations, null);
        }
    }

    public static void validateAll(Object obj, Class<?> clazz, List<FieldValidation> fieldValidations) {
        List<ValidationException.FieldError> errors = new ArrayList<>();

        for (FieldValidation fv : fieldValidations) {
            try {
                Field field = findField(clazz, fv.fieldName());
                if (field == null) {
                    errors.add(new ValidationException.FieldError(fv.fieldName(), "Field not found in class or superclasses"));
                    continue;
                }

                field.setAccessible(true);
                Object value = field.get(obj);

                for (ValidationType vt : fv.validations()) {
                    ValidationException.FieldError error = performValidation(fv.fieldName(), value, vt, fv.enumClass());
                    if (error != null) {
                        errors.add(error);
                    }
                }
            } catch (IllegalAccessException e) {
                errors.add(new ValidationException.FieldError(fv.fieldName(), "Field access error: " + e.getMessage()));
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    public static void validatePartial(Object obj, Class<?> clazz, List<FieldValidation> fieldValidations) {
        List<ValidationException.FieldError> errors = new ArrayList<>();

        for (FieldValidation fv : fieldValidations) {
            try {
                Field field = findField(clazz, fv.fieldName());
                if (field == null) {
                    errors.add(new ValidationException.FieldError(fv.fieldName(), "Field not found in class or superclasses"));
                    continue;
                }

                field.setAccessible(true);
                Object value = field.get(obj);

                if (value != null) {
                    for (ValidationType vt : fv.validations()) {
                        ValidationException.FieldError error = performValidation(fv.fieldName(), value, vt, fv.enumClass());
                        if (error != null) {
                            errors.add(error);
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                errors.add(new ValidationException.FieldError(fv.fieldName(), "Field access error: " + e.getMessage()));
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    public static Field findField(Class<?> clazz, String fieldName) {
        if (clazz == null) {
            return null;
        }

        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return findField(clazz.getSuperclass(), fieldName);
        }
    }

    private static ValidationException.FieldError performValidation(String fieldName, Object value, ValidationType type, Class<? extends Enum<?>> enumClass) {
        switch (type) {
            case NOT_NULL:
                if (value == null) {
                    return new ValidationException.FieldError(fieldName, "must not be null");
                }
                break;
            case NOT_EMPTY:
                if (value == null || (value instanceof Collection<?> && ((Collection<?>) value).isEmpty())) {
                    return new ValidationException.FieldError(fieldName, "must not be empty");
                }
                if (value instanceof String && ((String) value).isEmpty()) {
                    return new ValidationException.FieldError(fieldName, "must not be empty");
                }
                break;
            case NOT_BLANK:
                if (value == null || (value instanceof String && ((String) value).isBlank())) {
                    return new ValidationException.FieldError(fieldName, "must not be blank");
                }
                break;
            case VALID_EMAIL:
                if (!(value instanceof String) || !((String) value).matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                    return new ValidationException.FieldError(fieldName, "must be a valid email");
                }
                break;
            case POSITIVE:
                if (value == null) {
                    return new ValidationException.FieldError(fieldName, "must not be null");
                }
                if (value instanceof Number) {
                    if (value instanceof BigDecimal) {
                        if (((BigDecimal) value).compareTo(BigDecimal.ZERO) <= 0) {
                            return new ValidationException.FieldError(fieldName, "must be positive");
                        }
                    } else if (((Number) value).doubleValue() <= 0) {
                        return new ValidationException.FieldError(fieldName, "must be positive");
                    }
                } else {
                    return new ValidationException.FieldError(fieldName, "must be a positive number");
                }
                break;
            case ENUM_VALUE:
                if (value == null) {
                    return new ValidationException.FieldError(fieldName, "must not be null");
                }
                if (enumClass == null) {
                    break;
                }
                boolean valid = false;
                for (Enum<?> constant : enumClass.getEnumConstants()) {
                    if (constant.name().equals(value.toString())) {
                        valid = true;
                        break;
                    }
                }
                if (!valid) {
                    return new ValidationException.FieldError(fieldName, "must be one of: " + enumValuesString(enumClass));
                }
                break;
            default:
                break;
        }
        return null;
    }

    public static List<ValidationException.FieldError> validateCriterion(Criterion criterion) {
        if (criterion == null) {
            return List.of(new ValidationException.FieldError("criterion", "must not be null or empty"));
        }

        List<ValidationException.FieldError> errors = new ArrayList<>();

        String name = criterion.getName();
        if (name == null || name.isBlank()) {
            errors.add(new ValidationException.FieldError("name", "must not be blank"));
        }

        if (!CRITERIA_NAMES.contains(name)) {
            errors.add(new ValidationException.FieldError("name", "must not be a reserved keyword: " + String.join(", ", CRITERIA_NAMES)));
        }

        if (criterion.getWeight() <= 0) {
            errors.add(new ValidationException.FieldError("weight", "must be positive"));
        }

        if (criterion.getDetails() == null) {
            errors.add(new ValidationException.FieldError("details", "must not be null"));
        } else {
            ValidationException.FieldError fieldError= validateCriterionDetails(name, criterion.getDetails());
            if (fieldError != null) {
                errors.add(fieldError);
            }
        }

        if (!errors.isEmpty()) {
            return errors;
        }

        return null;
    }

    private static String enumValuesString(Class<? extends Enum<?>> enumClass) {
        Enum<?>[] constants = enumClass.getEnumConstants();
        List<String> names = new ArrayList<>();
        for (Enum<?> constant : constants) {
            names.add(constant.name());
        }
        return String.join(", ", names);
    }

    private static ValidationException.FieldError validateCriterionDetails(String name, Criterion.CriterionDetails details) {
        if (details == null) {
            return new ValidationException.FieldError("CriterionDetails", "must not be null");
        }

        String type = details.getType();
        if (type == null) {
            return new ValidationException.FieldError("CriterionDetails.type", "must not be null");
        }

        if (!Criterion.CriterionType.isValidType(type)){
            return new ValidationException.FieldError("CriterionDetails.type", "must be one of: " + String.join(", ", CRITERION_DETAILS_TYPES));
        }

        Criterion.CriterionType criterionType = Criterion.CriterionType.valueOf(type);
        switch (criterionType) {
            case ANY:
                return null;
            case ENUMERATION:
                if (details.getOptions() == null || details.getOptions().isEmpty()) {
                    return new ValidationException.FieldError("CriterionDetails.options",
                            "options must not be null or empty for ENUMERATION type");
                }


                Map<String, Object> enumClassMap = checkClassFieldInCriteria(name);
                if (!enumClassMap.isEmpty() && (Boolean) enumClassMap.get(CRITERIA_MAP_IS_VALID_ENUM)) {
                    Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) enumClassMap.get(CRITERIA_MAP_CLASS_NAME);
                    for (String option : details.getOptions()) {
                        boolean isValidEnum = false;
                        for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
                            if (enumConstant.name().equalsIgnoreCase(option)) {
                                isValidEnum = true;
                                break;
                            }
                        }
                        if (!isValidEnum) {
                            return new ValidationException.FieldError("CriterionDetails.options",
                                    "option '" + option + "' is not a valid enum value for " + enumClass.getSimpleName());
                        }
                    }
                    return null;
                }

                if (!enumClassMap.isEmpty() && (Boolean) enumClassMap.get(CRITERIA_MAP_IS_NUMERIC)) {
                    Class<?> fieldType = (Class<?>) enumClassMap.get(CRITERIA_MAP_CLASS_NAME);
                    for (String option : details.getOptions()) {
                        try {
                            if (fieldType == BigDecimal.class) {
                                new BigDecimal(option);
                            } else if (fieldType == Integer.class || fieldType == int.class) {
                                Integer.parseInt(option);
                            } else if (fieldType == Long.class || fieldType == long.class) {
                                Long.parseLong(option);
                            } else if (fieldType == Float.class || fieldType == float.class) {
                                Float.parseFloat(option);
                            } else if (fieldType == Double.class || fieldType == double.class) {
                                Double.parseDouble(option);
                            } else if (fieldType == Byte.class || fieldType == byte.class) {
                                Byte.parseByte(option);
                            } else if (fieldType == Short.class || fieldType == short.class) {
                                Short.parseShort(option);
                            }
                        } catch (NumberFormatException e) {
                            return new ValidationException.FieldError("CriterionDetails.options",
                                    "option '" + option + "' is not a valid " + fieldType.getSimpleName());
                        }
                    }
                }
            case RANGE:
                if (details.getMinValue() == null && details.getMaxValue() == null) {
                    return new ValidationException.FieldError("CriterionDetails.minValue/maxValue",
                            "at least one of minValue or maxValue must be specified for RANGE type");
                }

                if (details.getMinValue() != null && details.getMaxValue() != null) {
                    if (details.getMinValue().compareTo(details.getMaxValue()) > 0) {
                        return new ValidationException.FieldError("CriterionDetails.minValue/maxValue",
                                "minValue must be less than or equal to maxValue");
                    }
                }
                return null;

            default:
                return new ValidationException.FieldError("CriterionDetails.type",
                        "unsupported criterion type: " + type);
        }
    }

    private static Map<String, Object> checkClassFieldInCriteria(String fieldName) {
        Map<String, Object> map = new HashMap<>();
        try {
            Field field = findField(Criteria.class, fieldName);
            map.put(CRITERIA_MAP_CLASS_NAME, field.getType());
            Class<?> type = field.getType();
            map.put(CRITERIA_MAP_IS_NUMERIC, isNumericFieldInCriteria(type));
            map.put(CRITERIA_MAP_IS_VALID_ENUM, isEnumFieldInCriteria(fieldName, field));
        } catch (Exception e) {
            // Ignore exceptions
            // TODO: Add logging here
        }
        return map;
    }

    public static boolean isEnumFieldInCriteria(String fieldName, Field field) {
        return field != null && field.getType().isEnum() && findField(Enum.class, fieldName) != null;
    }

    private static boolean isNumericFieldInCriteria(Class<?> type) {
        return Number.class.isAssignableFrom(type) || type == int.class || type == long.class || type == float.class ||
                type == double.class || type == byte.class || type == short.class || type == BigDecimal.class;
    }

    public static List<String> getEnumValuesFromClass(Class<?> enumClass) {
        if (enumClass == null) {
            return Collections.emptyList();
        }

        if (!enumClass.isEnum()) {
            return Collections.emptyList();
        }

        Enum<?>[] constants = (Enum<?>[]) enumClass.getEnumConstants();
        List<String> names = new ArrayList<>();
        for (Enum<?> constant : constants) {
            names.add(constant.name());
        }
        return names;
    }
}
