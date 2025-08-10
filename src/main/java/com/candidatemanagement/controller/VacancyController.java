package com.candidatemanagement.controller;

import com.candidatemanagement.dto.CandidateRankingDto;
import com.candidatemanagement.dto.VacancyRequestDto;
import com.candidatemanagement.model.Vacancy;
import com.candidatemanagement.service.CandidateRankingService;
import com.candidatemanagement.service.VacancyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vacancies")
@CrossOrigin(origins = "*")
public class VacancyController {
    
    private final VacancyService vacancyService;
    private final CandidateRankingService candidateRankingService;

    public VacancyController(VacancyService vacancyService, CandidateRankingService candidateRankingService) {
        this.vacancyService = vacancyService;
        this.candidateRankingService = candidateRankingService;
    }
    
    @PostMapping
    public ResponseEntity<Vacancy> createVacancy(@RequestBody VacancyRequestDto vacancyDto) {
        Vacancy createdVacancy = vacancyService.createVacancy(vacancyDto);
        return new ResponseEntity<>(createdVacancy, HttpStatus.CREATED);
    }
    
    @GetMapping
    public ResponseEntity<List<Vacancy>> getAllVacancies() {
        List<Vacancy> vacancies = vacancyService.getAllVacancies();
        return new ResponseEntity<>(vacancies, HttpStatus.OK);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Vacancy> getVacancyById(@PathVariable String id) {
        return vacancyService.getVacancyById(id)
                .map(vacancy -> new ResponseEntity<>(vacancy, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @PatchMapping("/{id}")
    public ResponseEntity<Vacancy> updateVacancy(@PathVariable String id, @RequestBody VacancyRequestDto vacancyDto) {
        Vacancy updatedVacancy = vacancyService.updateVacancy(id, vacancyDto);
        return new ResponseEntity<>(updatedVacancy, HttpStatus.OK);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVacancy(@PathVariable String id) {
        vacancyService.deleteVacancy(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    
    @GetMapping("/{vacancyId}/rank-candidates")
    public ResponseEntity<List<CandidateRankingDto>> rankCandidatesForVacancy(@PathVariable String vacancyId) {
        List<CandidateRankingDto> rankedCandidates = candidateRankingService.rankCandidatesForVacancy(vacancyId);
        return new ResponseEntity<>(rankedCandidates, HttpStatus.OK);
    }
}
