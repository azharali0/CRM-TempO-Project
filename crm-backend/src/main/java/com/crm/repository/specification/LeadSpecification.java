package com.crm.repository.specification;

import com.crm.model.entity.Lead;
import com.crm.model.enums.LeadStage;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class LeadSpecification {

    private LeadSpecification() {
    }

    public static Specification<Lead> hasTitle(String title) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("title")), "%" + escapeLike(title.toLowerCase()) + "%");
    }

    public static Specification<Lead> hasStage(LeadStage stage) {
        return (root, query, cb) ->
                cb.equal(root.get("stage"), stage);
    }

    public static Specification<Lead> hasMinValue(BigDecimal minValue) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("value"), minValue);
    }

    public static Specification<Lead> hasMaxValue(BigDecimal maxValue) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("value"), maxValue);
    }

    public static Specification<Lead> hasMinProbability(Integer minProbability) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("probability"), minProbability);
    }

    public static Specification<Lead> ownedBy(UUID ownerId) {
        return (root, query, cb) -> {
            var join = root.join("owner", jakarta.persistence.criteria.JoinType.LEFT);
            return cb.equal(join.get("id"), ownerId);
        };
    }

    public static Specification<Lead> ownedByIn(List<UUID> ownerIds) {
        return (root, query, cb) -> {
            var join = root.join("owner", jakarta.persistence.criteria.JoinType.LEFT);
            return join.get("id").in(ownerIds);
        };
    }

    public static Specification<Lead> hasCustomer(UUID customerId) {
        return (root, query, cb) -> {
            var join = root.join("customer", jakarta.persistence.criteria.JoinType.LEFT);
            return cb.equal(join.get("id"), customerId);
        };
    }

    public static Specification<Lead> expectedCloseBefore(LocalDate date) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("expectedCloseDate"), date);
    }

    public static Specification<Lead> expectedCloseAfter(LocalDate date) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("expectedCloseDate"), date);
    }

    public static Specification<Lead> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) ->
                cb.between(root.get("createdAt"), from, to);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
    }
}
