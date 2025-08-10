package com.candidatemanagement.enums;

public enum Gender {
    MALE,
    FEMALE;

    public static Gender fromString(String value) {
        if (value == null) return null;
        for (Gender gender : Gender.values()) {
            if (gender.name().equalsIgnoreCase(value)) {
                return gender;
            }
        }
        return null;
    }
}
