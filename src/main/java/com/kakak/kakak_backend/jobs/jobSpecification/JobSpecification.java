package com.kakak.kakak_backend.jobs.jobSpecification;

import com.kakak.kakak_backend.jobs.jobEntity.Job;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JobSpecification {

    public static Specification<Job> filterJobs(
            String keyword,
            String city,
            String category,
            String employmentType,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            Timestamp shiftStartAfter,
            Timestamp shiftEndBefore,
            String status,
            UUID employerId
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String likeKeyword = "%" + keyword.toLowerCase() + "%";
                Predicate titlePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), likeKeyword);
                Predicate descPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likeKeyword);
                predicates.add(criteriaBuilder.or(titlePredicate, descPredicate));
            }

            if (city != null && !city.isBlank()) {
                String likeCity = "%" + city.toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("locationName")), likeCity));
            }

            if (category != null && !category.isBlank()) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("category")), category.toLowerCase()));
            }

            if (employmentType != null && !employmentType.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("employmentType"), employmentType));
            }

            if (salaryMin != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("salaryAmount"), salaryMin));
            }

            if (salaryMax != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("salaryAmount"), salaryMax));
            }

            if (shiftStartAfter != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("shiftStart"), shiftStartAfter));
            }

            if (shiftEndBefore != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("shiftEnd"), shiftEndBefore));
            }

            if (status != null && !status.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (employerId != null) {
                predicates.add(criteriaBuilder.equal(root.get("employer").get("id"), employerId));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
