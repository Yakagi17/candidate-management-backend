package com.candidatemanagement.model;

import com.candidatemanagement.model.criteria.Criteria;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import com.candidatemanagement.enums.Gender;

import java.math.BigDecimal;
import java.time.LocalDate;

@Document(collection = "candidates")
public class Candidate extends Criteria {
    
    @Id
    private String id;
    
    private String name;
    
    @Indexed(unique = true)
    private String email;

    public Candidate(String name, String email, LocalDate birthdate, Gender gender, BigDecimal currentSalary) {
        super(birthdate, gender, currentSalary);
        this.name = name;
        this.email = email;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
}
