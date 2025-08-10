package com.candidatemanagement.model.criteria;

import com.candidatemanagement.enums.Gender;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

public class Criteria {

    private LocalDate birthdate;

    private Gender gender;

    private BigDecimal currentSalary;

    private transient int age;

    public Criteria(LocalDate birthdate, Gender gender, BigDecimal currentSalary) {
        this.birthdate = birthdate;
        this.gender = gender;
        this.currentSalary = currentSalary;
    }

    public static final List<String> CUSTOM_CRITERIA_FIELDS = List.of("age");

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(LocalDate birthdate) {
        this.birthdate = birthdate;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public BigDecimal getCurrentSalary() {
        return currentSalary;
    }

    public void setCurrentSalary(BigDecimal currentSalary) {
        this.currentSalary = currentSalary;
    }

    public int getAge() {
        return Period.between(birthdate, LocalDate.now()).getYears();
    }
}
