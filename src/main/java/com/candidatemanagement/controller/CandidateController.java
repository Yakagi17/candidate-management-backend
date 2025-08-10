package com.candidatemanagement.controller;

import com.candidatemanagement.model.Candidate;
import com.candidatemanagement.service.CandidateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.candidatemanagement.dto.CandidateRequestDto;

import java.util.List;

@RestController
@RequestMapping("/api/v1/candidates")
@CrossOrigin(origins = "*")
public class CandidateController {
    
    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }
    
    @PostMapping
    public ResponseEntity<Candidate> createCandidate(@RequestBody CandidateRequestDto candidateDto) {
        Candidate createdCandidate = candidateService.createCandidate(candidateDto);
        return new ResponseEntity<>(createdCandidate, HttpStatus.CREATED);
    }
    
    @GetMapping
    public ResponseEntity<List<Candidate>> getAllCandidates() {
        List<Candidate> candidates = candidateService.getAllCandidates();
        return new ResponseEntity<>(candidates, HttpStatus.OK);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Candidate> getCandidateById(@PathVariable String id) {
        return candidateService.getCandidateById(id)
                .map(candidate -> new ResponseEntity<>(candidate, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @PatchMapping("/{id}")
    public ResponseEntity<Candidate> updateCandidate(@PathVariable String id, @RequestBody CandidateRequestDto candidateDto) {
        Candidate updatedCandidate = candidateService.updateCandidate(id, candidateDto);
        return new ResponseEntity<>(updatedCandidate, HttpStatus.OK);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCandidate(@PathVariable String id) {
        candidateService.deleteCandidate(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
