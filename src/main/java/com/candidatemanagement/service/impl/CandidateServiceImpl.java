package com.candidatemanagement.service.impl;

import com.candidatemanagement.dto.CandidateRequestDto;
import com.candidatemanagement.enums.Gender;
import com.candidatemanagement.exception.GenericApiException;
import com.candidatemanagement.model.Candidate;
import com.candidatemanagement.repository.CandidateRepository;
import com.candidatemanagement.service.CandidateService;
import com.candidatemanagement.utils.ValidationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class CandidateServiceImpl implements CandidateService {

    private final CandidateRepository candidateRepository;

    public CandidateServiceImpl(CandidateRepository candidateRepository) {
        this.candidateRepository = candidateRepository;
    }

    @Override
    public Candidate createCandidate(CandidateRequestDto candidateDto) {
        validateCreate(candidateDto);

        if (candidateRepository.existsByEmail(candidateDto.email())) {
            throw new GenericApiException(
                HttpStatus.CONFLICT,
                "Duplicate Email",
                "Candidate with email " + candidateDto.email() + " already exists"
            );
        }

        Candidate candidate = mapDtoToEntity(candidateDto, null);
        return candidateRepository.save(candidate);
    }

    @Override
    public List<Candidate> getAllCandidates() {
        return candidateRepository.findAll();
    }

    @Override
    public Optional<Candidate> getCandidateById(String id) {
        return candidateRepository.findById(id);
    }

    @Override
    public Candidate updateCandidate(String id, CandidateRequestDto candidateDto) {
        Candidate existing = candidateRepository.findById(id)
                .orElseThrow(() -> new GenericApiException(
                    HttpStatus.NOT_FOUND,
                    "Resource Not Found",
                    "Candidate with id " + id + " not found"
                ));

        validateUpdate(candidateDto);

        if (candidateDto.email() != null &&
            !existing.getEmail().equals(candidateDto.email()) &&
            candidateRepository.existsByEmail(candidateDto.email())) {
            throw new GenericApiException(
                HttpStatus.CONFLICT,
                "Duplicate Email",
                "Candidate with email " + candidateDto.email() + " already exists"
            );
        }

        Candidate updated = mapDtoToEntity(candidateDto, existing);
        return candidateRepository.save(updated);
    }

    @Override
    public void deleteCandidate(String id) {
        if (!candidateRepository.existsById(id)) {
            throw new GenericApiException(
                HttpStatus.NOT_FOUND,
                "Resource Not Found",
                "Candidate with id " + id + " not found"
            );
        }
        candidateRepository.deleteById(id);
    }

    private void validateCreate(CandidateRequestDto dto) {
        ValidationUtils.validateAll(
            dto,
            CandidateRequestDto.class,
            Arrays.asList(
                new ValidationUtils.FieldValidation("name", List.of(ValidationUtils.ValidationType.NOT_NULL, ValidationUtils.ValidationType.NOT_BLANK)),
                new ValidationUtils.FieldValidation("email", List.of(ValidationUtils.ValidationType.NOT_NULL, ValidationUtils.ValidationType.NOT_BLANK, ValidationUtils.ValidationType.VALID_EMAIL)),
                new ValidationUtils.FieldValidation("birthdate", List.of(ValidationUtils.ValidationType.NOT_NULL)),
                new ValidationUtils.FieldValidation("gender", List.of(ValidationUtils.ValidationType.NOT_NULL, ValidationUtils.ValidationType.ENUM_VALUE), Gender.class),
                new ValidationUtils.FieldValidation("currentSalary", List.of(ValidationUtils.ValidationType.NOT_NULL, ValidationUtils.ValidationType.POSITIVE))
            )
        );
    }

    private void validateUpdate(CandidateRequestDto dto) {
        List<ValidationUtils.FieldValidation> validations = new java.util.ArrayList<>();
        if (dto.name() != null)
            validations.add(new ValidationUtils.FieldValidation("name", List.of(ValidationUtils.ValidationType.NOT_BLANK)));
        if (dto.email() != null)
            validations.add(new ValidationUtils.FieldValidation("email", List.of(ValidationUtils.ValidationType.NOT_BLANK, ValidationUtils.ValidationType.VALID_EMAIL)));
        if (dto.birthdate() != null)
            validations.add(new ValidationUtils.FieldValidation("birthdate", List.of()));
        if (dto.gender() != null)
            validations.add(new ValidationUtils.FieldValidation("gender", List.of(ValidationUtils.ValidationType.ENUM_VALUE), Gender.class));
        if (dto.currentSalary() != null)
            validations.add(new ValidationUtils.FieldValidation("currentSalary", List.of(ValidationUtils.ValidationType.POSITIVE)));

        if (!validations.isEmpty()) {
            ValidationUtils.validatePartial(dto, CandidateRequestDto.class, validations);
        }
    }

    private Candidate mapDtoToEntity(CandidateRequestDto dto, Candidate entity) {
        if (entity == null) {
            return new Candidate(
                dto.name(),
                dto.email(),
                dto.birthdate(),
                Gender.fromString(dto.gender()),
                dto.currentSalary()
            );
        }
        if (dto.name() != null) entity.setName(dto.name());
        if (dto.email() != null) entity.setEmail(dto.email());
        if (dto.birthdate() != null) entity.setBirthdate(dto.birthdate());
        if (dto.gender() != null) entity.setGender(Gender.fromString(dto.gender()));
        if (dto.currentSalary() != null) entity.setCurrentSalary(dto.currentSalary());
        return entity;
    }
}
