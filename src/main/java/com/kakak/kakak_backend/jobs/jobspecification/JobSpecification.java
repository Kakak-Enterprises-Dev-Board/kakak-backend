package com.kakak.kakak_backend.jobs.jobspecification;

import com.kakak.kakak_backend.jobs.jobentity.Job;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JobSpecification {

    public record FilterParams(
            String keyword,
            String city,
            String category,
            String employmentType,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            Instant shiftStartAfter,
            Instant shiftEndBefore,
            String status,
            UUID employerId
    ) {}

    public static Specification<Job> filterJobs(FilterParams params) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            addKeywordPredicate(root, cb, predicates, params.keyword());
            addCityPredicate(root, cb, predicates, params.city());
            addCategoryPredicate(root, cb, predicates, params.category());
            addEmploymentTypePredicate(root, cb, predicates, params.employmentType());
            addSalaryPredicates(root, cb, predicates, params.salaryMin(), params.salaryMax());
            addShiftPredicates(root, cb, predicates, params.shiftStartAfter(), params.shiftEndBefore());
            addStatusPredicate(root, cb, predicates, params.status());
            addEmployerPredicate(root, cb, predicates, params.employerId());

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addKeywordPredicate(Root<Job> root, CriteriaBuilder cb, List<Predicate> predicates, String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            String likeKeyword = "%" + keyword.toLowerCase() + "%";
            Predicate titlePredicate = cb.like(cb.lower(root.get("title")), likeKeyword);
            Predicate descPredicate = cb.like(cb.lower(root.get("description")), likeKeyword);
            predicates.add(cb.or(titlePredicate, descPredicate));
        }
    }

    private static void addCityPredicate(Root<Job> root, CriteriaBuilder cb, List<Predicate> predicates, String city) {
        if (city != null && !city.isBlank()) {
            String likeCity = "%" + city.toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(root.get("locationName")), likeCity));
        }
    }

    private static void addCategoryPredicate(Root<Job> root, CriteriaBuilder cb, List<Predicate> predicates, String category) {
        if (category != null && !category.isBlank()) {
            predicates.add(cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
        }
    }

    private static void addEmploymentTypePredicate(Root<Job> root, CriteriaBuilder cb, List<Predicate> predicates, String employmentType) {
        if (employmentType != null && !employmentType.isBlank()) {
            predicates.add(cb.equal(root.get("employmentType"), employmentType));
        }
    }

    private static void addSalaryPredicates(Root<Job> root, CriteriaBuilder cb, List<Predicate> predicates, BigDecimal salaryMin, BigDecimal salaryMax) {
        if (salaryMin != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("salaryAmount"), salaryMin));
        }
        if (salaryMax != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("salaryAmount"), salaryMax));
        }
    }

    private static void addShiftPredicates(Root<Job> root, CriteriaBuilder cb, List<Predicate> predicates, Instant shiftStartAfter, Instant shiftEndBefore) {
        if (shiftStartAfter != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("shiftStart"), shiftStartAfter));
        }
        if (shiftEndBefore != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("shiftEnd"), shiftEndBefore));
        }
    }

    private static void addStatusPredicate(Root<Job> root, CriteriaBuilder cb, List<Predicate> predicates, String status) {
        if (status != null && !status.isBlank()) {
            predicates.add(cb.equal(root.get("status"), status));
        }
    }

    private static void addEmployerPredicate(Root<Job> root, CriteriaBuilder cb, List<Predicate> predicates, UUID employerId) {
        if (employerId != null) {
            predicates.add(cb.equal(root.get("employer").get("id"), employerId));
        }
    }
}
