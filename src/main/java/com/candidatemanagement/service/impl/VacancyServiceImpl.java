package com.candidatemanagement.service.impl;

import com.candidatemanagement.dto.VacancyRequestDto;
import com.candidatemanagement.exception.GenericApiException;
import com.candidatemanagement.exception.ValidationException;
import com.candidatemanagement.model.Vacancy;
import com.candidatemanagement.model.criteria.Criterion;
import com.candidatemanagement.repository.VacancyRepository;
import com.candidatemanagement.service.VacancyService;
import com.candidatemanagement.utils.ValidationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

@Service
public class VacancyServiceImpl implements VacancyService {

    private final VacancyRepository vacancyRepository;

    public VacancyServiceImpl(VacancyRepository vacancyRepository) {
        this.vacancyRepository = vacancyRepository;
    }

    @Override
    public Vacancy createVacancy(VacancyRequestDto vacancyDto) {
        validateCreate(vacancyDto);
        Vacancy vacancy = mapDtoToEntity(vacancyDto, null);
        return vacancyRepository.save(vacancy);
    }

    @Override
    public List<Vacancy> getAllVacancies() {
        return vacancyRepository.findAll();
    }

    @Override
    public Optional<Vacancy> getVacancyById(String id) {
        return vacancyRepository.findById(id);
    }

    @Override
    public Vacancy updateVacancy(String id, VacancyRequestDto vacancyDto) {
        Vacancy existing = vacancyRepository.findById(id)
                .orElseThrow(() -> new GenericApiException(
                        HttpStatus.NOT_FOUND,
                        "Vacancy Not Found",
                        "Vacancy with id " + id + " not found"
                ));

        validateUpdate(vacancyDto);

        Vacancy updatedVacancy = mapDtoToEntity(vacancyDto, existing);
        return vacancyRepository.save(updatedVacancy);
    }

    @Override
    public void deleteVacancy(String id) {
        if (!vacancyRepository.existsById(id)) {
            throw new GenericApiException(
                HttpStatus.NOT_FOUND,
                "Vacancy Not Found",
                "Vacancy with id " + id + " not found"
            );
        }
        vacancyRepository.deleteById(id);
    }

    private void validateCreate(VacancyRequestDto dto) {
        ValidationUtils.validateAll(
            dto,
            VacancyRequestDto.class,
            Arrays.asList(
                new ValidationUtils.FieldValidation("name", List.of(ValidationUtils.ValidationType.NOT_BLANK)),
                new ValidationUtils.FieldValidation("criteria", List.of(ValidationUtils.ValidationType.NOT_EMPTY))
            ));

        List<ValidationException.FieldError> errors = new ArrayList<>();
        for (Criterion criterion : dto.criteria()) {
            List<ValidationException.FieldError> criterionErrors = ValidationUtils.validateCriterion(criterion);
            if (criterionErrors != null && !criterionErrors.isEmpty()) {
                errors.addAll(criterionErrors);
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private void validateUpdate(VacancyRequestDto dto) {
        List<ValidationUtils.FieldValidation> validations = new ArrayList<>();

        if (dto.name() != null)
            validations.add(new ValidationUtils.FieldValidation("name", List.of(ValidationUtils.ValidationType.NOT_BLANK)));
        if (dto.criteria() != null) {
            validations.add(new ValidationUtils.FieldValidation("criteria", List.of(ValidationUtils.ValidationType.NOT_EMPTY)));
        }

        if (!validations.isEmpty()) {
            ValidationUtils.validatePartial(dto, VacancyRequestDto.class, validations);
        }

        if (dto.criteria() == null || dto.criteria().isEmpty()) {
            return;
        }

        List<ValidationException.FieldError> errors = new ArrayList<>();
        for (Criterion criterion : dto.criteria()) {
            List<ValidationException.FieldError> criterionErrors = ValidationUtils.validateCriterion(criterion);
            if (criterionErrors != null && !criterionErrors.isEmpty()) {
                errors.addAll(criterionErrors);
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private Vacancy mapDtoToEntity(VacancyRequestDto dto, Vacancy entity) {
        if (entity == null) {
            entity = new Vacancy(
                dto.name(),
                dto.criteria()
            );
        } else {
            if (dto.name() != null) entity.setName(dto.name());
            if (dto.criteria() != null) entity.setCriteria(dto.criteria());

        }
        return entity;
    }
}
