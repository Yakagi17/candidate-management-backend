package com.candidatemanagement.model;

import com.candidatemanagement.model.criteria.Criterion;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Set;

@Document(collection = "vacancies")
public class Vacancy {
    
    @Id
    private String id;
    
    private String name;
    
    private Set<Criterion> criteria;

    public Vacancy(String name, Set<Criterion> criteria) {
        this.name = name;
        this.criteria = criteria;
    }
    
    // Getters and Setters
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
    
    public Set<Criterion> getCriteria() {
        return criteria;
    }
    
    public void setCriteria(Set<Criterion> criteria) {
        this.criteria = criteria;
    }
    
    @Override
    public String toString() {
        return "Vacancy{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", criteria=" + criteria +
                '}';
    }
}
